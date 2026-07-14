# 🟨 Yellows 🟨
Yellows is a pipeline engine powered by a directed acyclic graph ( DAG ). Built for maximum load. It navigates heavy traffic with **no overtaking**.

## Features
* **Graph compiler** Identifies race conditions and optimizes the graph structure to ensure maximum runtime performance 
* __Lock free*__ Unsynchronized threads where relentless work outweighs idle waiting
* **Data oriented** Pedal to the metal. Leveraging Data-Oriented Design to bypass object-oriented bloat and extract maximum throughput from modern CPUs.
* **Extendable** Designed not to be all in one solution but to give foundation for implementing anything
* **Routines** Do not repeat yourself, write your favorite pipelines once and embed them everywhere else

## Tech
* **Java 25**
* **fastutils**
* **Jackson**
* **JUnit**
* **Mockito**

## Install and run
in near future

## Usage
To start using you only need run config in JSON format. For now, it looks like this
```json
{
  "nodes": [
    {
      "name" : "print-start",
      "plugin": "builtin.print",
      "input" : { "out" : "const.start" },
      "output" : {},
      "next" : ["hello-user"]
    },
    {
      "name": "hello-user",
      "routine": "print-formated",
      "input": {
        "format": "const.format",
        "name" : "const.who"
      },
      "output": {},
      "next": ["print-end"]
    },
    {
      "name" : "print-end",
      "plugin": "builtin.print",
      "input" : { "out" : "result" },
      "output" : {},
      "next" : []
    }
  ],
  "constants" : {
    "format": "Hello %s!!!",
    "who": "friend",
    "start" : "Start",
    "end": "End"
  },
  "routines": {
    "print-formated": [
      {
        "name": "format",
        "plugin": "builtin.format",
        "input": {
          "format_string": "in.format",
          "values.[0]": "in.name"
        },
        "output": {
          "result": "result"
        },
        "next": ["print"]
      },
      {
        "name": "print",
        "plugin": "builtin.print",
        "input" : {
          "out": "result"
        },
        "output": {},
        "next": []
      }
    ]
  }
}
```
**Note**:
* node_name should be unique per node
* plugin_id is unique id defined by plugin itself
* Path is split into segments by "." where segment "[...]" will be parsed as array index
* If the plugin provides navigation hints, they will be checked to see if that navigation is allowed in next; otherwise, execution will be stopped.
* If there is two or more nodes they will be run in parallel and also will be validated for race condition
* All nodes that don't have any in connections will be marked as roots and used to start race condition validation and to start executing
* All constants mapped into `const` branch and can be shadowed
* All routine inputs mapped into `in` branch and can be shadowed
* Const don't map into routines context

## Plugins
Yellows supports loading external .jar plugins, but they should match requirements
* Class should be marked with Plugin annotation where will be defined plugin id and scope ( Singleton ( Shared )/ Per request)
* Class should not be abstract or interface
* Class should implement PluginNode interface
* Class should have public no args constructor
* Jar resources should have *plugin.txt* where in each line will be defined plugin class where plugin will be searched, example:
```txt
org.example.MyPluginClass1
org.example.MyPluginClass2
```
* Incorrect plugin will stop all execution with the corresponding error

Note
* Default plugin scope is shared
* The best way to create trigger node is to use *completeAndSpawn* callback that will copy context and start and will allow this node to continue executing safely. Calling completeAndReturn more than once is not recommended, because it may trigger inner graph corruption (execution will be stopped) or future silent context corruption (both will be fixed in near future with implementation of fail fast double return detection)

### Built in plugins
* `builtin.print`: inputs - `out`, outputs -
* `builtin.if`: inputs - `a_name`, `b_name`, `condition`, outputs -
* `builtin.format`: inputs - `format_string`, `values`, outputs - `result`
* `builtin.delete`: inputs - `key`, outputs - 
* `builtin.math.add`: inputs - `a`, `b`, outputs - `out`
* `builtin.math.subtract`: inputs - `a`, `b`, outputs - `out`
* `builtin.math.multiply`: inputs - `a`, `b`, outputs - `out`
* `builtin.math.divide`: inputs - `a`, `b`, outputs - `out`

## Some boring implementation details
Graph compilation is a process that can be divided into two key stages: 1—optimization; 2—validation
### Optimisation
The main goal of optimization is to map most strings to integers while storing the data in the most efficient structures. Thus, any global paths are converted to their numerical representations; for example, a.[0] is converted to [0, -2147483648] (array indices are stored using the index | 0x80000000). Node names are first converted to numbers (regardless of paths); the most efficient approach is to reconstruct the string from the number, as this involves an array search, whereas converting a string to a number uses a hash map (see the SymbolTable implementation for details). Later, when storing the graph in forward star format, they are flattened into a linear array, so we obtain the following hierarchy: `node_name` → `node_global_id` → `node_local_id`. This path, like the paths themselves, is optimized only for ascending order, where `local_id` → `global_id` uses an additional array in the subgraph, and `global_id` → `node_name` uses the array in SymbolTable. Note: Conversion via a hash map is used in the hot path only to execute plugin transition hints, and in cases where a plugin reads a path stored in an int context—most often, this is possible only if the plugin’s context corresponds to large subtrees that were not created by a single record from another plugin.
### Validation
The most important part of graph validation is checking for race conditions. To do this, we use the concept of authorship: when a plugin writes something to a specific key, it becomes the author of that key. If, when merging contexts, the authors do not match, we can definitively say that we have encountered a race condition. This is implemented using a CoW Trie, where each node is a segment of a key. So, when two branches extend from a node, they inherit the parent’s context and make changes there; at some point, they may converge again at a single node (regardless of the configuration, a special node is injected at this moment specifically to catch such branches at a single merge point), and here an intersection check is performed according to the following rules
* A author not equals B author
* A has reader and B has writer deeper (And vice versa)
* Continue deeper into children if not found

After that, if no conflicts are found, the contexts are merged and passed on.
Although Trie uses CoW and hash maps based on primitives, it is still one of the most time-consuming parts of the algorithm, since during a write operation the entire map of child nodes must be copied, even though only one reference among them will be replaced.
### Context
Context is a set of classes and abstractions designed to represent a tree-like structure with dynamic types. The main optimization here is the division into string-based and int-based objects; thus, the global tree composed of config paths attempts to be int-based for best performance, but this can be changed with the ability to return nested structures and overwrite keys.  
The implementation of paths (IntPath/StringPath) allows us to easily convert segments to the required type, regardless of the key type or the type of object we’re using, by utilizing the SymbolTable. Here are a couple of cases where this approach would be inefficient, both from the engine’s perspective and from the plugin’s perspective.
* Plugin overrides context root, then all lookups using inner int path will require converting through array
* Plugin receives as input all context root (it is still int object tree with only primitives leafs ), then any lookup in plugin will require using map

As a result, Case 2 turns out to be the most painful.  
**Is it worth it?**  
I can definitely say **yes**, since it allows us to use an int hash map for the most part, while also providing an additional performance boost in IntPath. The IntPath implementation stores the segment array as an `int[]`, which allows it to utilize the processor’s LRU cache and provide significant acceleration on the hot path during iteration (this optimization originated in the graph validator but was eventually ported to the context as well).

Note: IntPath iterator reuses the same segment.
### Lock free run context
To ensure optimal performance with parallel run context branches, the implementation is completely lock-free*. To ensure optimal performance with parallel threads, the run context is designed to be completely lock-free*. This is achieved because race conditions have already been verified and the context does not require synchronization; furthermore, since the graph is stored in the `inDegree` array, it can be used as an `AtomicIntegerArray`. The rest of the structures required for operation are read-only and do not require synchronization.  
It is worth mentioning the implementation of node state storage in `inDegree`, since the cancellation of a thread and its request may occur in different orders. To ensure that cancellation does not remove nodes that were supposed to be executed upon request by another thread, an `isWanted` flag has been added, which, as in the example with indexes, is the 31st bit of a 32-bit int.  
*Unfortunately, despite the bold claims of a “lock-free” run context, it is not actually lock-free; it must synchronize during copying, since it is not possible to safely copy both the context and inDegree. But this is only the case when trigger spawns new run context.
### Routines
Think about routines as independent runs of subgraph. Each routine define its own `RunContext` which allows it to be completely independent as in context as in using node names.  
Be aware that all context in `in` branch are immutable and will only be shadowed by any changes.
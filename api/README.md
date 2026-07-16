# 🟨 Yellows 🟨 api
This module contains all api needed to create plugin

## Content
* **Context**
  * PluginWriteWrapper
  * PluginReadWrapper
  * GlobalContextFactory
* **Plugin**
  * Plugin
  * PluginCallback
  * PluginNode
  * PluginScope

## Creating plugin
Yellows plugin is just a concrete class that have `@Plugin` annotation and implements `PluginNode` interface  
**Example**
```java
@Plugin(
        id = "example.example", // Unique plugin id, used in config
        scope = PluginScope.SHARED  // Plugin scope, default SHARED aka singleton, also available PER_INVOCATION
)
public class Example implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        /*
                This is used if you don't want to stop current plugin but want to continue pipeline.
                It will copy current RunContext (with locking) and continue execution there
         */
        cb.completeAndSpawn(GlobalContextFactory.createMissing(), List.of());
        /*
                To continue current context pipeline plugin MUST call completeAndReturn/fail
                Without that executor will kill runContext with ExecutorError ERR_CALLBACK_NOT_INVOKED
                
                Note. It is only allowed to call completeAndReturn once. If you try to do it multiple times you will receive IllegalStateException.
                It is believed that this is terminal action of plugin and after that it will not interact with the context, so after that all calls of completeAndSpawn will throw IllegalStateException
         */
        cb.completeAndReturn(GlobalContextFactory.createMissing(), List.of());
        
        /*
                Used to return from plugin with exception. But don't worry if you forget to handle exception and push it with fail, plugin wrapper will handle it and do it by itself.
                
                Note. This action mark plugin as finished and all rules from completeAndReturn is present here
                Note. Failed plugin will kill runContext so pipeline can't continue work
         */
        cb.fail(new RuntimeException(":("));
    }
}
```
## Working with context
Plugin can work with context using two interfaces
* PluginReadWrapper
* PluginWriteWrapper

`PluginReadWrapper` is provided to the plugin and contains all keys mapped as input, it made fully immutable and I recommend you to keep it immutable  
It provides methods for
* determining type
* getting primitive value
* resolving paths
* executing math operations (only for `Int`, `Long`, `Float`, `Double`, `Boolean` values, other will result in receiving `NaN` value)
* iterating (only implemented for `Objects` and `Arrays`)

  
`PluginWriteWrapper` created to be used in plugin and return to Context
It provides methods for
* putting different primitives
* putting values received in `PluginReadWrapper` (Note. It will use deep copy of that value)
* marking key for deletion (current `deleteKey` method, maybe I will change name to more corresponding)

But wait, it is interface, and don't have any constructors or static methods, so how do I supposed to get it?
You can use `GlobalContextFactory` it provides methods for creating Object, Array and MissingValue (It is singleton)

## Providing next hints
Hints for next node is that List<String> in completeAndReturn, you should pass there next node names.  
Note.
Empty list will be treated as default which means running all nodes declared in next.
These names must match what is declared in node next otherwise runContext will be killed with ExecutorException with code ERR_ILLEGAL_TRANSITION.  
I recommend not to hard code this names so they should be repeated in config but to receive them as input.  
For example builtin if plugin
```java
@Plugin(id = "builtin.if")
public class If implements PluginNode {
    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        String aBranch = input.resolvePath("a_name").asString();
        String bBranch = input.resolvePath("b_name").asString();

        PluginReadWrapper condition = input.resolvePath("condition");

        boolean convertedCondition = (condition.isString() && !condition.asString().isEmpty()) ||
                condition.asLong(0) > 0    ||
                condition.asInt(0) > 0     ||
                condition.asFloat(0) > 0   ||
                condition.asDouble(0) > 0  ||
                condition.asBoolean(false) ||
                condition.isArray()        ||
                condition.isObject();

        if (convertedCondition) {
            cb.completeAndReturn(GlobalContextFactory.createMissing(), List.of(aBranch));
        } else {
            cb.completeAndReturn(GlobalContextFactory.createMissing(), List.of(bBranch));
        }
    }
}
```
## P.S
You may notice that api module provides ContextFactoryProvider interface. It is used by engine to provide suppliers for object, array, and missing wrappers via SPI. It is not recommended to declare you own realization and provide them via SPI, engine uses methods to unwrap them where it checks if provided class is its own implementation, if it is not then IllegalArgumentException will be thrown.
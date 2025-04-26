# Oh, hello fellow viewer ... or programmer?
Here you can find different program modules.  

Always use this to send a (debug) message to the console:
```java
WorldSystem.logger().log(Level.INFO,"Debug message");
```
  
---

- [WorldSystem.java](WorldSystem.java) represents the Main file. Here the plugin get's the start and stop information.
- [WorldCheckerRunnable.java](WorldCheckerRunnable.java) Checks if the world folder is in good condition
- [GCRunnable.java](GCRunnable.java) Adds a Thread to run different processes
- [wrapper](wrapper)
  - [AsyncCreatorAdapter.java](wrapper/AsyncCreatorAdapter.java) Option to load the minecraft worlds async
  - [GeneratorSettings.java](wrapper/GeneratorSettings.java) 
  - [SystemWorld.java](wrapper/SystemWorld.java)
  - [WorldPlayer.java](wrapper/WorldPlayer.java)
  - [WorldTemplate.java](wrapper/WorldTemplate.java)
  - [WorldTemplateProvider.java](wrapper/WorldTemplateProvider.java)
- [util](util)
- [listener](listener)
- [guicreate](guicreate)
- [gui](gui)
- [events](events)
- [database](database)
- [config](config)
- [commands](commands)

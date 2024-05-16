# WorldPainter Demo Plugin

This is a skeleton project to illustrate how to create plugins for WorldPainter.

## Possibilities

The plugin mechanism of WorldPainter currently allows you to provide:

* New singular, unconfigurable layer types
* New custom layer types, of which the user can create multiple and configure each differently
* New tools for the Tools panel, that are applied to a location or area of the map using mouse or tablet
  * In future, it will also be possible to provide global tools which are not localised and are not applied using mouse or tablet
* New custom object file formats for use with Custom Object layers
* New map formats, to be able to Export to or Import from other file formats than Minecraft Java Edition

## Usage

To use this project, copy it to a new project and take the following steps:

1. Install the non-public WorldPainter dependencies in your local Maven repository following [these](https://github.com/Captain-Chaos/WorldPainter/blob/master/BUILDING.md#installing-dependencies) instructions.
   - mvn install:install-file -Dfile=./jide-plaf-jdk7.jar -DgroupId=com.jidesoft -DartifactId=jide-plaf-jdk7 -Dversion=3.7.13 -Dpackaging=jar -DgeneratePom=true
   - Make sure the version are exactly the same as worldpainter WPGui requires them with jide.version! 3.7 is NOT 3.7.13!
1. Read all the comments, both Javadoc and regular.
1. Rename the package and class names as appropriate for your organisation and the plugin you want to create. Be careful also to update the configuration files that refer to them, all unique identifiers, etc. Otherwise, you may create clashes with other plugins.
1. Delete the classes and methods you don't need. There are examples of all types of plugins, most of which you won't need to create one particular plugin.
1. To test your plugin with WorldPainter, run the `test` Maven goal with the `testWithWorldPainter` profile active:
```shell
mvn test -P testWithWorldPainter
```

## Notes

**Note** that these classes are empty skeletons that don't actually _do_ anything. They are here only to illustrate the required structure of the code.

## Distribution

You can run your plugin locally for testing and debugging using the above instructions, but other WorldPainter installations won't load unsigned plugins. You will need to arrange for your plugin to be built and signed by the author of WorldPainter before you can distribute it, following these prerequisites and actions:

1. The project needs to be a straightforward Maven or Gradle project that can be built without additional or manual steps.
1. All dependencies need to be accessible to WorldPainter's private build server. They can be in a private repository, as long as access can be arranged with a private key or token.
1. The same applies to the source code. It needs to be accessible for inspection and for building by WorldPainter's private build server. It does not have to be public, as long as access can be arranged with a private key or token.
1. Contact the author of WorldPainter in order to set up the build and signing process. The process can be set up to be automatic whenever new commits are made and the binary can be delivered where you choose, e.g via email or an online storage service.
   1. On [Reddit](https://www.reddit.com/user/CaptainChaos74)
   1. On [Discord](https://discordapp.com/users/475648103312261131)
   1. Via [email](mailto:plugin-dev@pepsoft.org)
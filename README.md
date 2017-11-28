Courier
=======
Courier, the Discord Rich Presence integration for Dota 2! **Currently WIP**

Using
-----
Currently I am not distributing pre-compiled versions of Courier because I still think it's an incomplete
project and shouldn't be used in a production environment without further testing/development. But if you
really want to use it right now and you know how to use git and maven, feel free to clone the repo and follow
the `Setting up Development Environment` section and compile it with maven then use it. **IF YOU DO THIS PLEASE
REPORT ANY BUGS YOU FIND!**

Setting up Development Environment
----------------------------------
Download [This library](https://github.com/Vatuu/discord-rpc/releases/download/0.9-beta.2/discord-rpc-0.9-BETA-2.jar "Discord-RPC JNA Library")
and install it to your local maven repository, you can use this command:
```Bash
mvn install:install-file -Dfile=discord-rpc-0.9-BETA-2.jar -DgroupId=net.arikia.dev -DartifactId=discord-rpc -Dversion=0.9-BETA-2 -Dpackaging=jar
```
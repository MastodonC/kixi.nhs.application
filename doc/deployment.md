# Building the application
The application is built as a standalone JAR. To create a basic uberjar, a JAR containting your application code and all the necessary dependencies, we can call `lein uberjar`. The JAR will be output to target dir appended with `-standalone`. We can run this JAR via the usual Java way (described in the next section). To create the uberjar, please execute the steps below in the project's directory:
```sh
lein clean
lein compile
lein uberjar
```
# Running the application
Copy the application jar to the server and start it from the command line:
```sh
java -jar kixi.nhs.application-0.1.0-SNAPSHOT-standalone.jar
```
To make sure the application stays running when you log out of the box, run it in a tmux session:
```sh
tmux
java -jar kixi.nhs.application-0.1.0-SNAPSHOT-standalone.jar
```
Tmux session will remain open when you quit you log out of the box. Next time you log in to the box, you can come back to the same session:
```sh
tmux attach
```

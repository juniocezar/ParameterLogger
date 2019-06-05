# Parameter Logger

This tool is a Soot Extension that logs the values of all parameters and globals used by a method.

##### What does it do?
Right after entering a method, this pass collects the values of all parameters. Also, right before
the first use of any global, its value is collected. In every exit point of the method, we insert
instrumentation that logs the method's signature and all collected values. In the end, before the
instrumented application stops running, the logs are dumped into a file.

##### Sample
This pass would transform this:
```java
global static int check = 10;

public static void main (String[] args) {
    sample(args.length);
}

private static boolean sample (int length) {
    if (length > check) return true; return false;
}

```
... into something like this:

```java
global static int check = 10;

public static void main (String[] args) {
    sample(args.length);
    lac.jinn.exlib.dump();
}

private static boolean sample (int length) {
    lac.jinn.exlib.log("boolean sample (int length)", length, check);
    if (length > check) return true; return false;
}

```


## Installation

This is a self-contained repository, this way you should only needs to run the command:

```bash
./cmd.sh build
```

## Usage

```bash
./cmd.sh run $CLASS_PATH $CLASS_NAME
```

This commands looks for the class ($CLASS_NAME) in the class path ($CLASS_PATH) and run our tool
over it, generating an instrumented version inside the folder sootOutput.


## Repository Structure

```
./
| --- build :: classes of the compiled pass
| --- netbeans-project :: project's sources
| --- samples :: sample codes
```


## License
[GPL](https://www.gnu.org/licenses/gpl-3.0.pt-br.html)


# Usage
A good binscure config is made up of four main components:

## Input/Output
Example:
```Yaml
input: test.jar
output: test-obf.jar
```
Output is not required, and will default to [input]-obf.jar

## Libraries
Libraries are required for the remapper to work. If you have a class ClassA that extends ClassB, and ClassB is not 
present in the input jar, then a jar containing ClassB *must* be present in the libraries for binscure to function
normally. Binscure will not force you to include all referenced classes, however if you dont it can break
functionality.

Example:
```Yaml
libraries:
    - library1.jar
    - library2.jar
```

Unlike other obfuscators it is not necessary to include the JRE in the libraries.

## Exclusions
Binscure allows you to define a list of classes that will be totally excempt from all obfuscation.
This is made up of two types, exclusions and hard exclusions. 

Exclusions will be processsed by binscure, even if they are not "obfuscated". You should use this for classes
that are part of your program, but that you do not want obfuscated.

Example:
```Yaml
exclusions:
    - com/binclub/binscure/Main # This is a class
    - com/binclub/binscure/api/ # This is a package
```

Hard exclusions will not be processed whatsoever, reducing the total time taken to obfuscate. You should use this
for third party libraries included in your final jar.

Example:
```Yaml
hardExclusions:
    - javassist/
    - com/google/
    - kotlin/
```

## Transformers
Example:
```Yaml
remap: # Renames classes, fields, and methods
    enabled: false
    classes: true
    methods: true
    fields: true
    classPrefix: ""
    methodPrefix: ""
    fieldPrefix: ""
    aggressiveOverloading: false # Will aggressively overload member names with different descriptions

sourceStrip: # Removes debugging information left by compilers
    enabled: false
    lineNumbers: REMOVE (REMOVE/KEEP)

kotlinMetadata: # Removes metadata left by the kotlin compiler used for kotlinx-reflection
    enabled: false

crasher: # Uses certain tricks to completely crash some decompilers and disassemblers
    enabled: false

indirection: # Hides method calls
    enabled: false

stringObfuscation: # Hides string constants
    enabled: false

flowObfuscation: # Makes method flow hard to follow both manually and through control flow graphs
    enabled: false
    severity: HARD # (NORMAL, HARD, SEVERE, AGGRESSIVE)
```

## Other options
There are some other extra options that you can set:
```Yaml
# If set to true you will not be warned about classes not being found in the libraries
ignoreClassPathNotFound: false

# A file path where a .csv mappings file will be saved
mappingFile: null
```

===========================================
Steps to complete this trialpack template:
===========================================

0.) Ensure you have dependencies JDK (1.5 or later) and Maven (3.3.9 or later).

1.) Open src/test/java/TestSuite.java and:

  1.a.) Select an appropriate return type for the makeTestable() method that 
        matches the signature of the method in which you plan to 
        synthesize code.

        For example, if you plan to synthesize inside the body of:

             public int countLines(String text) { ... }

        set the return type of makeTestable to Function<String,Integer>.

  1.b.) Author one or more JUnit test cases in TestSuite that make use of
        the result of makeTestable().

2.) Open src/test/java/PassProgram.java and implement the interface returned
    by makeTestable() defined in step 1.a.  This implementation should pass
    all test cases defined in TestSuite.java

3.) Create any number of draft Java programs in src/test/resources that
    implement the same interface as PassProgram in step 2.

    3.a.) Leave any number of holes to be synthesized in each draft source file
          using "/// #" notation where each # is unique within a single file.

          E.g. /// 1
               /// 2
               ...

4.) Open src/test/resources/trails.xml and define any number of synthesis
    trials to be evaluated.

    One trial equates to one synthesis run of the specified draft program
    (see trails.xml comments for detailed format examples).
    Each <draftProgramPath> node's value should be relative to
    src/test/resources and reference a file within src/test/resources.

    Each hole id attribute of a <hole> element must correspond to a # used in
    step 3.a. within the related <draftProgramPath>.

5.) Save all files and execute:

        $ ./build

    from the root directory to create the trial pack.

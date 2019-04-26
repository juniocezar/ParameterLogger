PROJECTDIR=netbeans-project
SOOTLIB=$(PROJECTDIR)/lib/sootclasses-trunk-jar-with-dependencies.jar

all:
	mkdir -p build
	javac -d build -cp $(SOOTLIB) $(PROJECTDIR)/src/lac/jinn/*.java $(PROJECTDIR)/src/lac/jinn/*/*.java

run:
	java -cp $(PROJECTDIR)/lib/sootclasses-trunk-jar-with-dependencies.jar:build lac.jinn.ProfilerPassDriver -w -cp build:/home/juniocezar/Apps/java8/jre/lib/rt.jar:samples  Case1

dacapo:
	java -cp $(PROJECTDIR)/lib/sootclasses-trunk-jar-with-dependencies.jar:build lac.jinn.ProfilerPassDriver -w -cp build:/home/juniocezar/Apps/java8/jre/lib/rt.jar:samples -app --process-dir benchmarks/dacapo-9.12-MR1-bach.jar

clean:
	rm -r build runtime-logs soot-out1 soot-out2 preditor-dir

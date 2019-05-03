PROJECTDIR=netbeans-project
SOOTLIB=$(PROJECTDIR)/lib/sootclasses-trunk-jar-with-dependencies.jar

all:
	mkdir -p build
	javac -d build -cp $(SOOTLIB) $(PROJECTDIR)/src/lac/jinn/*.java $(PROJECTDIR)/src/lac/jinn/*/*.java

run:
	java -cp $(PROJECTDIR)/lib/sootclasses-trunk-jar-with-dependencies.jar:build lac.jinn.ProfilerPassDriver -w -cp build:/home/juniocezar/Apps/java8/jre/lib/rt.jar:samples  Case1

dacapo:
	java -cp $(PROJECTDIR)/lib/sootclasses-trunk-jar-with-dependencies.jar:build lac.jinn.ProfilerPassDriver -w -cp benchmarks/dacapo-9.12-MR1-bach.jar:build:/home/juniocezar/Apps/java8/jre/lib/rt.jar:samples -app -include org.apache. -include org.w3c. -main-class Harness Harness

avrora:
	java -cp $(PROJECTDIR)/lib/sootclasses-trunk-jar-with-dependencies.jar:build lac.jinn.ProfilerPassDriver -w -cp benchmarks/dacapo-9.12-MR1-bach.jar:build:/home/juniocezar/Apps/java8/jre/lib/rt.jar:samples -app -include org.apache. -include org.w3c. --process-dir benchmarks/dacapo/jar/original-avrora-cvs-20091224.jar -d sootOutput/avrora-cvs-20091224.jar -outjar

clean:
	rm -r build runtime-logs soot-out1 soot-out2 preditor-dir

JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
    UDPReceiver.java \
    UDPNetwork.java \
    UDPSender.java \

default: clean classes 

classes: $(CLASSES:.java=.class)

.PHONY: clean
clean:
	rm -f *.class

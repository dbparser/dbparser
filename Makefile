DEPTH = 	../..

ODIR =		.
LOCAL_PACKAGES =	danbikel.util danbikel.util.proxy danbikel.lisp \
			danbikel.switchboard \
			danbikel.parser \
			danbikel.parser.english danbikel.parser.chinese \
			danbikel.parser.util

PACKAGES = 	$(LOCAL_PACKAGES) danbikel.wordnet

JARNAME =	db-parser
JARDIR =	${HOME}/public_html

# the following to be defined only by a recursive call to make
PACKAGE =

# the following four variables should almost never be changed
PACKAGE_PATH =	$(subst .,/,$(PACKAGE))
OBJDIR_EXTENSION = 	.classes
OBJDIR =	$(PACKAGE_PATH)$(OBJDIR_EXTENSION)
SRCDIR =	src/$(PACKAGE_PATH)

# java executables
JAR =		jar
JAVAC =		jikes
JAVADOC =	javadoc
RMIC =		rmic

JAVAC_FLAGS = +E

# Unix executables
RM =		/bin/rm

MODEL_STRUCTURE_SOURCES = $(wildcard $(SRCDIR)/*ModelStructure*.java)
MODEL_STRUCTURE_CLASSES = $(addprefix $(OBJDIR)/,$(patsubst %.java,%.class,$(notdir $(MODEL_STRUCTURE_SOURCES))))

danbikel.util.CLASSES =	$(OBJDIR)/AbstractFixedSizeList.class \
			$(OBJDIR)/AbstractMapToPrimitive.class \
			$(OBJDIR)/AllPass.class \
			$(OBJDIR)/Debug.class \
			$(OBJDIR)/Filter.class \
			$(OBJDIR)/FixedSizeArrayList.class \
			$(OBJDIR)/FixedSizeListFactory.class \
			$(OBJDIR)/FixedSizeList.class \
			$(OBJDIR)/FixedSizeSingletonList.class \
			$(OBJDIR)/FlexibleMap.class \
			$(OBJDIR)/HashMap.class \
			$(OBJDIR)/HashMapDouble.class \
			$(OBJDIR)/HashMapInt.class \
			$(OBJDIR)/HashMapPrimitive.class \
			$(OBJDIR)/HashMapTwoDoubles.class \
			$(OBJDIR)/HashMapTwoInts.class \
			$(OBJDIR)/IntCounter.class \
			$(OBJDIR)/IntPair.class \
			$(OBJDIR)/JarClassLoader.class \
			$(OBJDIR)/JarRunner.class \
			$(OBJDIR)/MapToPrimitive.class \
			$(OBJDIR)/ObjectBank.class \
			$(OBJDIR)/ObjectPool.class \
			$(OBJDIR)/Pair.class \
			$(OBJDIR)/SLNode.class \
			$(OBJDIR)/Stack.class \
			$(OBJDIR)/Text.class \
			$(OBJDIR)/Time.class \
			$(OBJDIR)/TimeoutServerSocket.class \
			$(OBJDIR)/TimeoutSocket.class \
			$(OBJDIR)/TimeoutSocketFactory.class

danbikel.util.proxy.CLASSES =	$(OBJDIR)/Reconnect.class \
				$(OBJDIR)/Retry.class

danbikel.lisp.CLASSES =	$(OBJDIR)/IntSymbol.class \
			$(OBJDIR)/Sexp.class \
			$(OBJDIR)/SexpConvertible.class \
			$(OBJDIR)/SexpList.class \
			$(OBJDIR)/SexpTokenizer.class \
			$(OBJDIR)/StringSymbol.class \
			$(OBJDIR)/Symbol.class \
			$(OBJDIR)/WordTokenizer.class

danbikel.switchboard.CLASSES =	$(OBJDIR)/AbstractClient.class \
				$(OBJDIR)/AbstractServer.class \
				$(OBJDIR)/AbstractSwitchboardUser.class \
				$(OBJDIR)/Client.class \
				$(OBJDIR)/DefaultNoHeaderObjectWriter.class \
				$(OBJDIR)/DefaultObjectReader.class \
				$(OBJDIR)/DefaultObjectReaderFactory.class \
				$(OBJDIR)/DefaultObjectWriter.class \
				$(OBJDIR)/DefaultObjectWriterFactory.class \
				$(OBJDIR)/Failover.class \
				$(OBJDIR)/NumberedObject.class \
				$(OBJDIR)/ObjectReader.class \
				$(OBJDIR)/ObjectReaderFactory.class \
				$(OBJDIR)/ObjectWriter.class \
				$(OBJDIR)/ObjectWriterFactory.class \
				$(OBJDIR)/RegistrationException.class \
				$(OBJDIR)/Server.class \
				$(OBJDIR)/Switchboard.class \
				$(OBJDIR)/SwitchboardUser.class \
				$(OBJDIR)/SwitchboardRemote.class \
				$(OBJDIR)/TextObjectWriter.class \
				$(OBJDIR)/TextObjectWriterFactory.class \
				$(OBJDIR)/UnrecognizedClientException.class \
				$(OBJDIR)/UnrecognizedServerException.class

danbikel.switchboard.STUBS =	$(OBJDIR)/AbstractSwitchboardUser_Stub.class \
				$(OBJDIR)/Switchboard_Stub.class

danbikel.parser.CLASSES =	$(MODEL_STRUCTURE_CLASSES) \
				$(OBJDIR)/AbstractEvent.class \
				$(OBJDIR)/BaseNPAwareShifter.class \
				$(OBJDIR)/BiCountsTable.class \
				$(OBJDIR)/Chart.class \
				$(OBJDIR)/CKYChart.class \
				$(OBJDIR)/CKYItem.class \
				$(OBJDIR)/Collins.class \
				$(OBJDIR)/Constants.class \
				$(OBJDIR)/Constraint.class \
				$(OBJDIR)/ConstraintSetFactory.class \
				$(OBJDIR)/ConstraintSet.class \
				$(OBJDIR)/ConstraintSets.class \
				$(OBJDIR)/CountsTable.class \
				$(OBJDIR)/CountsTrio.class \
				$(OBJDIR)/Decoder.class \
				$(OBJDIR)/DecoderServer.class \
				$(OBJDIR)/DecoderServerRemote.class \
				$(OBJDIR)/DefaultShifter.class \
				$(OBJDIR)/Event.class \
				$(OBJDIR)/GapEvent.class \
				$(OBJDIR)/HeadEvent.class \
				$(OBJDIR)/HeadFinder.class \
				$(OBJDIR)/HeadTreeNode.class \
				$(OBJDIR)/Item.class \
				$(OBJDIR)/Language.class \
				$(OBJDIR)/Model.class \
				$(OBJDIR)/ModelCollection.class \
				$(OBJDIR)/ModifierEvent.class \
				$(OBJDIR)/MutableEvent.class \
				$(OBJDIR)/Nonterminal.class \
				$(OBJDIR)/Parser.class \
				$(OBJDIR)/ParserRemote.class \
				$(OBJDIR)/PriorEvent.class \
				$(OBJDIR)/ProbabilityCache.class \
				$(OBJDIR)/ProbabilityStructure.class \
				$(OBJDIR)/Settings.class \
				$(OBJDIR)/SexpEvent.class \
				$(OBJDIR)/SexpNumberedObjectReader.class \
				$(OBJDIR)/SexpNumberedObjectReaderFactory.class \
				$(OBJDIR)/SexpObjectReader.class \
				$(OBJDIR)/SexpObjectReaderFactory.class \
				$(OBJDIR)/SexpSubcatEvent.class \
				$(OBJDIR)/Shift.class \
				$(OBJDIR)/Shifter.class \
				$(OBJDIR)/SingletonWordList.class \
				$(OBJDIR)/StartSwitchboard.class \
				$(OBJDIR)/Subcat.class \
				$(OBJDIR)/SubcatBag.class \
				$(OBJDIR)/SubcatBagFactory.class \
				$(OBJDIR)/SubcatFactory.class \
				$(OBJDIR)/SubcatList.class \
				$(OBJDIR)/SubcatListFactory.class \
				$(OBJDIR)/Subcats.class \
				$(OBJDIR)/SymbolicCollectionWriter.class \
				$(OBJDIR)/SymbolPair.class \
				$(OBJDIR)/Trainer.class \
				$(OBJDIR)/TrainerEvent.class \
				$(OBJDIR)/Training.class \
				$(OBJDIR)/Transition.class \
				$(OBJDIR)/Treebank.class \
				$(OBJDIR)/UnlexTreeConstraint.class \
				$(OBJDIR)/UnlexTreeConstraintSetFactory.class \
				$(OBJDIR)/UnlexTreeConstraintSet.class \
				$(OBJDIR)/Word.class \
				$(OBJDIR)/WordFeatures.class \
				$(OBJDIR)/WordArrayList.class \
				$(OBJDIR)/WordList.class \
				$(OBJDIR)/WordListFactory.class

danbikel.parser.STUBS =		$(OBJDIR)/Parser_Stub.class \
				$(OBJDIR)/DecoderServer_Stub.class

danbikel.parser.RESOURCES =	$(OBJDIR)/default-settings.properties

danbikel.parser.english.CLASSES =	$(OBJDIR)/HeadFinder.class \
					$(OBJDIR)/SimpleWordFeatures.class \
					$(OBJDIR)/Training.class \
					$(OBJDIR)/Treebank.class \
					$(OBJDIR)/WordFeatures.class

danbikel.parser.english.RESOURCES =	$(OBJDIR)/data

danbikel.parser.chinese.CLASSES =	$(OBJDIR)/HeadFinder.class \
					$(OBJDIR)/SimpleWordFeatures.class \
					$(OBJDIR)/Training.class \
					$(OBJDIR)/Treebank.class \
					$(OBJDIR)/WordFeatures.class

danbikel.parser.chinese.RESOURCES =	$(OBJDIR)/data

danbikel.parser.util.CLASSES =	$(OBJDIR)/AddFakePos.class \
				$(OBJDIR)/DebugChart.class \
				$(OBJDIR)/LispHead.class \
				$(OBJDIR)/ParseToSentence.class \
				$(OBJDIR)/PrettyPrint.class \
				$(OBJDIR)/TrainerEventToCollins.class \
				$(OBJDIR)/Util.class

DEP_PACKAGE_DIRS = $(DEPTH)/wn

include $(DEPTH)/java/make.rules

# for javadoc
DOCDIR = 	doc
#DOCSRCPATH =	'src:$(DEPTH)/wn/src'
DOCSRCPATH =	'src'
WINDOWTITLE =	'WordNet Parser'
DOCTITLE =	'WordNet Parser'
HEADER =	'WordNet Parser'
BOTTOM =	'Author: <a target="_blank" href="http://www.cis.upenn.edu/~dbikel">Dan Bikel</a>.'
LINKS =		-link '$(JDK1.4API_LINK)' -link '$(WORDNET_LINK)'
JAVADOC_PACKAGES = $(LOCAL_PACKAGES)

###############################################################################
# Targets to build

# N.B.: Local targets (not recursive make tagets like deppackages, below) should
# in general have objdir in their dependency lists.

.PHONY:	all lisp util parser localpackages package stubs deppackages install
.PHONY:	cleanall cleanlocal

all:	deppackages localpackages

# targets for the individual local packages
util:
	@make --no-print-directory package "PACKAGE=danbikel.util"
lisp:
	@make --no-print-directory package "PACKAGE=danbikel.lisp"
english:
	@make --no-print-directory package "PACKAGE=danbikel.parser.english"
parser:
	@make --no-print-directory package "PACKAGE=danbikel.parser"

localpackages:
	@for i in $(LOCAL_PACKAGES); do \
		echo "Building package $$i"; \
		make --no-print-directory package "PACKAGE=$$i"; \
	done

package:	objdir prebuild $($(PACKAGE).CLASSES) build stubs $($(PACKAGE).RESOURCES)

stubs:; @make --silent --no-print-directory $($(PACKAGE).STUBS)

deppackages:
	@for i in $(DEP_PACKAGE_DIRS); do \
		cd $$i; \
		make all; \
	done

install:
	@cd $(ODIR) ; \
	$(RM) -rf danbikel.tmp; \
	if [ -d danbikel ]; then \
		mv danbikel danbikel.tmp; \
	fi
	make localpackages "JAVAC_FLAGS = $(JAVAC_FLAGS) -O" "OBJDIR_EXTENSION="
	$(JAR) cvf $(JARDIR)/$(JARNAME).jar danbikel
	@cd $(ODIR) ; \
	if [ -d danbikel.tmp ]; then \
		$(RM) -rf danbikel; \
		mv danbikel.tmp danbikel; \
	fi

cleanall:	cleanlocal
	@for i in $(DEP_PACKAGE_DIRS); do \
		cd $$i; \
		make clean; \
	done

cleanlocal:
	@for i in $(LOCAL_PACKAGES); do \
		echo "Removing $$i"; \
		make --silent "PACKAGE=$$i" clean; \
	done

# To build a debugging target, type make <target>.debug

# To build an optimized target, type make <target>.opt
#
###############################################################################

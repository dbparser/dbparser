;;; a list of contexts in which children can be considered arguments
;;; (complements)
;;; the syntax is: (arg-contexts <context>+)
;;; where
;;; <context>      ::= (<parent> <child list>)
;;; <parent>       ::= the symbol of a parent nonterminal
;;; <child list>   ::= a list of symbols of child nonterminals s.t. when their
;;;                    parent is <parent>, they are candidates for being
;;;                    relabeled as arguments
;;;                    OR
;;;                    the list (head <integer>) where <integer> is the
;;;                    the amount to add to the head index (which can be
;;;                    negative)
(arg-contexts (S (NP SBAR S))
	      (VP (NP SBAR S VP))
	      (SBAR (S))
	      (PP (head 1)))
;;; a list of semantic tags on Penn Treebank nonterminals that prevent
;;; children in the appropriate contexts from being relabeled as arguments
(sem-tag-arg-stop-list (ADV VOC BNF DIR EXT LOC MNR TMP CLR PRP))

;;; a list of nodes to be pruned from training data parse trees
(prune-nodes (`` ''))

;;; THE FOLLOWING DATA IS CURRENTLY NOT USED BY danbikel.parser.english.Training

;;; a list of contexts in which baseNP's can occur
;;; the syntax is: (base-np <context>+)
;;; where
;;; <context>    ::= (<parent> (<child>+)) | (<parent> <context>)
;;; <parent>     ::= the symbol of a parent nonterminal
;;; <child>      ::= <childsym> | (not <childsym>)
;;; <childsym>   ::= the symbol of a child nonterminal
;;;
;;; where (not <childsym>) matches any symbol that is not <childsym>.
(base-np (NP ((not NP)))
	 (NP (NP (head POS))))

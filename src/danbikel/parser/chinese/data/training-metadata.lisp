;;; a list of contexts in which children can be considered arguments
;;; (complements)
;;; the syntax is: (arg-contexts <context>+)
;;; where
;;; <context>      ::= (<parent> <child list>)
;;;
;;; <parent>       ::= the symbol of a parent nonterminal
;;;
;;; <child list>   ::= <nt-list> | <head-list>
;;;
;;; <nt-list>      ::= (<nt>+)
;;;                    a list of symbols of child nonterminals s.t. when their
;;;                    parent is <parent>, they are candidates for being
;;;                    relabeled as arguments
;;;
;;; <nt>           ::= a nonterminal label (a symbol)
;;;
;;; <head-list>    ::= (head <integer>) |
;;;                    (head-pre <search-set>) |
;;;                    (head-post <search-set>)
;;;
;;; <integer>      ::= an integer that is the amount to add to the head index
;;;                    (this integer can also be negative, but not zero)
;;;                    e.g., (head 1) indicates that the first child after the
;;;                    head is a candidate for being relabeled as an argument
;;;
;;; <search-set>   ::= <direction> [<not>] <nt>+
;;;
;;; <direction>    ::= first | last
;;;                    first indicates a left-to-right search of the children
;;;                    of <parent>
;;;                    last indicates a right-to-left search
;;;                    e.g., (head-post first PP NP WHNP) indicates to perform
;;;                    a left-to-right search on the right side of the head,
;;;                    where the first child found whose label is one of
;;;                    {PP, NP, WHNP} will be a candidate for being relabeled
;;;                    as an argument
;;;
;;; <not>          ::= not
;;;                    indicates to search for something not in the set of
;;;                    nonterminals specified by <nt>+
;;;
(arg-contexts (VP (IP CP VP))
              (CP (IP CP))
              (PP (NP LCP QP DP IP CP UCP))
              (DNP (DP QP NP LCP PP ADJP UCP))
              (DVP (DP NP QP VP ADVP UCP))
              (LCP (NP QP DP LCP PP IP UCP))
              (* (*-SBJ *-OBJ *-IO *-PRD)))
;;; a list of semantic tags on treebank nonterminals that prevent
;;; children in the appropriate contexts from being relabeled as arguments
(sem-tag-arg-stop-list (ADV))

;;; a list of nodes to be pruned from training data parse trees
(prune-nodes (`` '' .))

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

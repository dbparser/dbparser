(setq homedir (getenv "HOME"))

(setq
 jde-global-classpath (list (concat ".:" homedir "/jgl3.1.0/lib/jgl3.1.0.jar:"
				    homedir "/wn:" homedir "/db-parser:"
				    homedir "/beanshell:"
				    homedir "/beanshell/bsh.jar"))
 jde-make-program "cd ~/db-parser/ ; make"
 jde-run-option-properties (list (cons "WNHOME" (getenv "WNHOME")))
 jde-compile-option-directory "~/db-parser"
 jde-compile-option-debug (quote ("all" (t t nil)))
 jde-run-option-heap-size (quote ((200 . "megabytes") (600 . "megabytes")))
 jde-db-source-directories (quote ("~/db-parser/src/danbikel/util/"
				   "~/db-parser/src/danbikel/lisp/"
				   "~/db-parser/src/danbikel/parser/english/"
				   "~/db-parser/src/danbikel/parser/"
				   "~/wn/src/wordnet/"))
 jde-db-option-properties (list (cons "WNHOME" (getenv "WNHOME")))
 jde-db-option-heap-size (quote ((200 . "megabytes") (600 . "megabytes"))))

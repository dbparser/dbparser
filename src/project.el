(let ((curr-file (file-truename (car file-name-history))))
  (string-match ".*/src/" curr-file)
  (setq src-root-dir (substring curr-file 0 (1- (match-end 0))))
  (setq sandbox-dir (file-truename (concat src-root-dir "/.."))))

(setq relative-src-dirs '("danbikel/lisp"
			  "danbikel/parser"
			  "danbikel/parser/english"
			  "danbikel/parser/util"
			  "danbikel/switchboard"
			  "danbikel/util"
			  "danbikel/util/proxy"))

(setq wn-src-dir '("~/wn/src/danbikel/wordnet"))

(setq absolute-src-dirs
      (append (mapcar '(lambda (arg) (concat src-root-dir "/" arg))
				relative-src-dirs)
	      wn-src-dir))

(setq
 jde-global-classpath (list (concat src-root-dir path-separator
				    (getenv "CLASSPATH")))
 jde-make-program (concat "cd " sandbox-dir "; make")
 jde-run-option-properties (list (cons "WNHOME" (getenv "WNHOME")))
 jde-compile-option-directory sandbox-dir
 jde-compile-option-debug (quote ("all" (t t nil)))
 jde-run-option-heap-size (quote ((200 . "megabytes") (600 . "megabytes")))
 jde-db-source-directories absolute-src-dirs
 jde-db-option-properties (list (cons "WNHOME" (getenv "WNHOME")))
 jde-db-option-heap-size (quote ((200 . "megabytes") (600 . "megabytes"))))

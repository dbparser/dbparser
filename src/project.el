(let ((this-file (file-truename (car file-name-history))))
  (string-match ".*/" this-file)
  (setq parent-dir (substring this-file 0 (1- (match-end 0)))))

(setq relative-src-dirs '("danbikel/lisp"
			  "danbikel/parser"
			  "danbikel/parser/english"
			  "danbikel/parser/util"
			  "danbikel/switchboard"
			  "danbikel/util"
			  "danbikel/util/proxy"))

(setq wn-src-dir '("~/wn/src/danbikel/wordnet"))

(setq absolute-src-dirs
      (append (mapcar '(lambda (arg) (concat parent-dir "/" arg))
				relative-src-dirs)
	      wn-src-dir))

(setq
 jde-global-classpath (list (concat parent-dir path-separator
				    (getenv "CLASSPATH")))
 jde-make-program (concat "cd " parent-dir "; make")
 jde-run-option-properties (list (cons "WNHOME" (getenv "WNHOME")))
 jde-compile-option-directory parent-dir
 jde-compile-option-debug (quote ("all" (t t nil)))
 jde-run-option-heap-size (quote ((200 . "megabytes") (600 . "megabytes")))
 jde-db-source-directories absolute-src-dirs
 jde-db-option-properties (list (cons "WNHOME" (getenv "WNHOME")))
 jde-db-option-heap-size (quote ((200 . "megabytes") (600 . "megabytes"))))

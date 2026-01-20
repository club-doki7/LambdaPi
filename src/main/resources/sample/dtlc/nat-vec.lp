defun Nat = _Tf_Nat
defun zero = _Tf_Zero
defun succ = λx. _Tf_Succ x : Nat -> Nat
defun natElim = λm. λz. λs. λn. _Tf_NatElim m z s n
              : ∀ (m : Nat -> *),
                m zero ->
                (∀ (k : Nat), m k -> m (succ k)) ->
                ∀ (n : Nat), m n

defun plus = natElim (λ_. Nat -> Nat)
                     (λx. x)
                     (λk. λrec. λn. succ (rec n))

defun 0 = zero
defun 1 = succ 0
defun 2 = succ 1
defun 3 = succ 2
defun 4 = succ 3

defun Vec = λa. λl. _Tf_Vec a l : ∀ (a : *), ∀ (l : Nat), *
defun nil = λa. _Tf_Nil a : ∀ (a : *) -> Vec a 0
defun cons = λa. λn. λx. λxs. _Tf_Cons a n x xs
            : ∀ (a : *), ∀ (n : Nat), a -> Vec a n -> Vec a (succ n)

defun vecElim = λa. λm. λnc. λic. λn. λv. _Tf_VecElim a m nc ic n v
               : ∀ (a : *),
                 ∀ (m : ∀ (n : Nat), Vec a n -> *),
                 m 0 (nil a) ->
                 (∀ (k : Nat),
                  ∀ (x : a),
                  ∀ (xs : Vec a k),
                  m k xs -> m (succ k) (cons a k x xs)) ->
                 ∀ (n : Nat), ∀ (v : Vec a n), m n v

defun append = λa. vecElim a
                           (λm. λ_. ∀ (n : Nat), Vec a n -> Vec a (plus m n))
                           (λn. λv. v)
                           (λm. λv. λvs. λrec. λn. λw. cons a (plus m n) v (rec n w))
             : ∀ (a : *),
               ∀ (m : Nat),
               ∀ (v : Vec a m),
               ∀ (n : Nat),
               ∀ (w : Vec a n),
               Vec a (plus m n)

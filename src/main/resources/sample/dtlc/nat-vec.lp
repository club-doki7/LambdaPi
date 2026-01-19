defun Nat = _Tf_Nat
defun zero = _Tf_Zero
defun succ = \x. _Tf_Succ x : Nat -> Nat
defun natElim = \m. \z. \s. \n. _Tf_NatElim m z s n
              : forall (m : Nat -> *),
                m zero ->
                (forall (k : Nat), m k -> m (succ k)) ->
                forall (n : Nat), m n

defun plus = natElim (\_. Nat -> Nat)
                     (\x. x)
                     (\k. \rec. \n. succ (rec n))

defun 0 = zero
defun 1 = succ 0
defun 2 = succ 1
defun 3 = succ 2
defun 4 = succ 3

defun Vec = \a. \l. _Tf_Vec a l : forall (a : *), forall (l : Nat), *
defun nil = \a. _Tf_Nil a : forall (a : *) -> Vec a 0
defun cons = \a. \n. \x. \xs. _Tf_Cons a n x xs
            : forall (a : *), forall (n : Nat), a -> Vec a n -> Vec a (succ n)

defun vecElim = \a. \m. \nc. \ic. \n. \v. _Tf_VecElim a m nc ic n v
               : forall (a : *),
                 forall (m : forall (n : Nat), Vec a n -> *),
                 m 0 (nil a) ->
                 (forall (k : Nat),
                  forall (x : a),
                  forall (xs : Vec a k),
                  m k xs -> m (succ k) (cons a k x xs)) ->
                  forall (n : Nat), forall (v : Vec a n), m n v

defun append = \a. vecElim a
                           (\m. \_. forall (n : Nat), Vec a n -> Vec a (plus m n))
                           (\n. \v. v)
                           (\m. \v. \vs. \rec. \n. \w. cons a (plus m n) v (rec n w))
             : forall (a : *),
               forall (m : Nat),
               forall (v : Vec a m),
               forall (n : Nat),
               forall (w : Vec a n),
               Vec a (plus m n)

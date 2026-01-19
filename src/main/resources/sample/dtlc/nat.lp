defun Nat = _Tf_Nat
defun zero = _Tf_Zero
defun succ = \x. _Tf_Succ x : Nat -> Nat
defun natElim = \m. \z. \s. \n. _Tf_NatElim m z s n
              : forall (m : Nat -> *),
                m zero ->
                (forall (k : Nat), m k -> m (succ k)) ->
                forall (n : Nat), m n

defun add = natElim (\_. Nat -> Nat)
                    (\x. x)
                    (\k. \rec. \n. succ (rec n))

defun 0 = zero
defun 1 = succ 0
defun 2 = succ 1
defun 3 = succ 2
defun 4 = succ 3

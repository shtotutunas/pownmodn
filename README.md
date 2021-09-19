It is program for finding solutions of equation a^n = b (mod n) for given a and b.

It's mostly based on ideas described on [this page](https://web.archive.org/web/20120104074313/http://www.immortaltheory.com/NumberTheory/2nmodn.htm).  

It was used for:
- finding 7th solution of equation 2^n = 13 (mod n) (OIES [A051446](https://oeis.org/A051446))
- finding 10th solution of equation 2^n = 5 (mod n) (OIES [A128121](https://oeis.org/A128121))

Some things that would be good to improve in case if i return to this code (TODO list):
- optimize scanning by reducing exponents in Common.pow()
- optimize Common.pow() function by using fewer multiplications, e.g. x^15 = (x^3)^5
- use Math.multiplyHigh() for larger set of numbers
- add sieving by quadratic residues, e.g. for 2^n-3 scan only 1, 5, 19, 23 (mod 24) and etc
- make Launcher.fastScan() mode faster
- check if it's possible to generate good primes faster, e.g. if use Index calculus algorithm for discrete logarithm 

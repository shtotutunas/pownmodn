It is program for finding solutions of equation a^n = b (mod n) for given a and b.

It's mostly based on ideas described on [this page](https://web.archive.org/web/20120104074313/http://www.immortaltheory.com/NumberTheory/2nmodn.htm).  

It was used for:
- finding 7th solution of equation 2^n = 13 (mod n) (OEIS [A051446](https://oeis.org/A051446))
- finding 10th solution of equation 2^n = 5 (mod n) (OEIS [A128121](https://oeis.org/A128121))
- finding 5th and 6th solutions of equation 3^n = 2 (mod n) (OEIS [A276671](https://oeis.org/A276671))
- finding 5th solution of equation 2^n = -11 (mod n) (OEIS [A334634](https://oeis.org/A334634))

Some things that would be good to improve in case if i return to this code (TODO list):
- add more comments :D
- try to make ModPowCalculator faster, e.g. by using Math.multiplyHigh() for bigger set of numbers and by using fewer multiplications in exponentiation
- add factorizer implementation that would use external GMP-ECM executable file

"""Regression checks for the two array input mappings used by update 1.2."""

rho_a = [29.4, 16.7, 12.7, 10.6, 8.58, 7.72, 6.74, 5.63, 4.92, 4.30, 3.82, 3.01, 3.33]
wenner_a = [0.5 * (i + 1) for i in range(13)]
schlumberger_ab2 = [1.5 * a for a in wenner_a]
schlumberger_mn = list(wenner_a)

assert wenner_a[0] == 0.5 and wenner_a[-1] == 6.5
assert schlumberger_ab2[0] == 0.75 and schlumberger_ab2[-1] == 9.75
assert schlumberger_mn[0] == 0.5 and schlumberger_mn[-1] == 6.5
assert rho_a[-2:] == [3.01, 3.33]

depths = []
depth = 0.0
for thickness in (0.3352, 1.874):
    depth += thickness
    depths.append(depth)
assert depths == [0.3352, 2.2092]
assert [-d for d in depths] == [-0.3352, -2.2092]

print("Array mapping and depth/altitude checks passed")

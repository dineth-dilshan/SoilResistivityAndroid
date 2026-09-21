import math

def apparent_resistivity(mn_half, ab_half, resistance):
    k = math.pi * (ab_half ** 2 - mn_half ** 2) / (2 * mn_half)
    return k, k * resistance

tests = [
    (0.25, 0.75, 29.4, 92.36),
    (0.50, 1.50, 16.7, 104.93),
    (3.00, 9.00, 3.33, 125.54),
]

for mn, ab, r, expected in tests:
    _, rho = apparent_resistivity(mn, ab, r)
    assert abs(rho - expected) < 0.01, (rho, expected)
    assert abs((2 * ab / 3) - (2 * mn)) < 1e-9  # Wenner a = AB/3 = MN

print("Formula and Wenner spacing tests passed")

from math import *


def calc_resp(m, ar):
    print('####################')
    # s_t = 0.00504
    # s_t = 0.00079
    s_t = 0.0019

    # s_t += (m-1)*0.001
    # s_t = 0.00807
    mu = 1 / s_t
    ru = ar / (mu * m)
    print 'utilization: ', ru
    sigma = 0
    for n in range(1, m - 1):
        sigma += (pow(m * ru, n) / factorial(n))
    p0 = 1 / (1 + (pow(m * ru, m) / (factorial(m) * (1 - ru))) + sigma)

    p_queue = pow(m * ru, m) * p0 / (factorial(m) * (1 - ru))
    print 'p_queue:', p_queue
    resp = (1 + (p_queue / (m * (1 - ru)))) / mu

    print 'MM1: ', 1/(mu*m - ar)
    return resp


if __name__ == '__main__':
    print calc_resp(2, 341)
    # print calc_resp(8, 824)
    #
    # print calc_resp(12, 937)

    # print calc_resp(8, 550)
package com.vismo.cablemeter.util

import com.vismo.cablemeter.BuildConfig

object Constant {
    const val ENV_DEV = "dev"
    const val ENV_QA = "qa"
    const val ENV_PROD = "prod"

    private const val BASE_APP_URL_DEV = "https://oapi.dev.dash-hk.com/"
    private const val BASE_APP_URL_QA =  "https://oapi.qa.dash-hk.com/"
    private const val BASE_APP_URL_PROD = "https://oapi.dash-hk.com/"
    val BASE_OAPI_URL = when (BuildConfig.FLAVOR) {
            ENV_QA -> BASE_APP_URL_QA
            ENV_PROD-> BASE_APP_URL_PROD
            else -> BASE_APP_URL_DEV
    }

    private const val BASE_API_URL_DEV = "https://api.dev.dash-hk.com/"
    private const val BASE_API_URL_QA = "https://api.qa.dash-hk.com/"
    private const val BASE_API_URL_PROD = "https://api.dash-hk.com/"
    val BASE_API_URL = when (BuildConfig.FLAVOR) {
            ENV_QA -> BASE_API_URL_QA
            ENV_PROD -> BASE_API_URL_PROD
            else ->  BASE_API_URL_DEV
    }

    const val PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIJKgIBAAKCAgEAz0iqJpro3ojTifm3pXo1AYQ/3f3AV5fwmpZU0vJzjxFkZ4jZ\n" +
            "fiKcxbOjUB5duDUxPCzifX81DWkHlX+90wwhQc40HBP37odOwuMM782I55YmgF44\n" +
            "d+svBzUWxJ9pWglYuqI2X9gtZg15E+Cr+deeN7Br/xku93vsv9u4BoANo8gT20p3\n" +
            "Uzp6U0X4ixNWPnCBPvj+iaF69qMT4knPRl8OB9QgIYpzSxA4FblgONx121ijaVUz\n" +
            "ZL+wSl8M+vtHxJD6ZTwvGXlAuBsK7Y2fu9V8p1NOmxoZJCKLGLGtny9eAd0L++Cr\n" +
            "OZ6ViXfwoz29n878t4ZdFEm3qh3Zo60MvpuxiFtj4SVq0rx3FEjCZU8gGZH1VJUs\n" +
            "WLWipq2L+khrf0MpspsNDjmE1bgHG5jES6AafrVa6mwVeE7gbGCLXyKH+BCPe4Tn\n" +
            "jBNlX4ryR5t58w13uQSS4wedNxPsBj+P70VahVKna3HC5NHds60YJ1ME4CJJfJxL\n" +
            "yPvJwPUWn08Ct2iNaqs56+cgVNR0MdanHUl5XBU6v6nHWZtYPau243MimQr6YU08\n" +
            "wWVrXBnLkxBjLBqyAWg4UMnL++rqqcRIiPDigOSf5ASXXlaDvDdVASX73KVTX3ZH\n" +
            "QUgPjoS0XG5Dm4x1qs9Ey2ROmSxaojsx3EmMvDFkf7GzJ7U7PQ6nb9/3JJkCAwEA\n" +
            "AQKCAgEAjHZXJtXuW24ouuTHN5E6fg6aINolDrZdSsP4MwIsoJROj4JCr6DQz3/N\n" +
            "eSs6ousYtAhOanBbCP1VME9h2LPtjWQoF6bIaZCzJIkraQzEavpqmK5AYbKzKZUm\n" +
            "GpNTlrmisMXfRs985BzZIUksAJ/GDUK1zCXKl9YYNmKzr2Py2jtoiT9K4NFNt0RB\n" +
            "Ci1OUf12969O/QC6DIuxm7Qcizk6jr+GDnR9cC5Lcx0tQRrTmpHUGjqsGudylNMY\n" +
            "PrMsjN0ZcKR53i82VgutOhCwX15pnBFtwNjMlJn72/37A1XbxDOAPWyfWupFtuPZ\n" +
            "4yShbLucsNiSFn9lwBd+NdW8PyXOBLp7vNhnuRLPurUfbyrKF3NTCwHpXAA46yml\n" +
            "jVgmW3v/Wz5tRGIueMKbzTJUw9tGvM8GBUz0VZeXFC9/7nGV4vrHBYdefG3F7nrs\n" +
            "ae0FjBYdD3Kgtr6RpBA/FZIETdpGx/eY3x2zFvgfQkovAkuXFod+5DqgSQdu4ynd\n" +
            "AEtd+B8KqDRv7Hcf8PsQJ07DChmumgoTulI41T3GfnvjniuAOQt3/be717C/Ud9L\n" +
            "01pZVWGSEvC8wcy+OCuWoZBOLUIQ4y1IM8xOkuyuGRTJSOPjbCEJqfD2j5Cc5Gpm\n" +
            "I4C4tiMNw5wH3nLZP7sbVkqDjBITiOYc6pD/IvBSPz2HsmB5SAECggEBAOoUQ8P3\n" +
            "BxiKCFdk0mx+J9BU8bQGm9i5YBVAcgbEFK867asCxdNCIKuI/uyCK9cjhfotMkY5\n" +
            "rUBj2DQ7vtye9lCkE3hfEJ1V5VbqE4In1bstZEA4Dzw3HZ8/9yix++4Jxh7lGb/k\n" +
            "CTV1LeqjWka6wPwJBMvlgipuJpEDse+YVmRfwjZBOp37l8Hfn7/kWHF1t8BOo6oy\n" +
            "zq98ZnR4qJR0t/LPo2Ays8IL7R1oojlicCgnnx4jx481ZiQde1awP4mmBVjpWs5e\n" +
            "z2DSPx4GnBc9t0s2KRGX2LZNGfadsq/ARIR1Hf5S6xI5e3ZHbvdrdcOj8YifarbC\n" +
            "HkCTdoclBgfgQRkCggEBAOKyBKILsTOPci0wN572RvJv6qxdp1FZIg5zc4SDEVSQ\n" +
            "mAK5wpMjRh9oiuCQt0B+SUYQGYEN9sZVrnSqfGXX6u4avhNbsYq60aUx5hX6++ap\n" +
            "QFnfZACpCGTBbFFF9yF4vUZQ2OQufvIowOrzLasmeTi8bcCIYZbw8gqEJulYoJqm\n" +
            "RzFIylUBIviWbLfmE7tuES8b1jywP/H1hpGVTsMoSlURf4thVKQ4WMz4P470YG/i\n" +
            "431OFdyXu5HMYRSyXiGCKASETFYJ5MwFl/e7W0krasPb04R9mbXgijXgoKSVTwIr\n" +
            "SLRVQL6YNU8Us9YW8yTHxGO5YEydPrj1HTd4XFqw74ECggEAGUFselVJ17T053EC\n" +
            "XyBRbWHs3i6kXALMQh687svzK2DhScU30O5elgNm9d+pmUHE79Q2i2snrT3YkvIr\n" +
            "SqSAEP1S+HbRVRhycSk6R53CqrpgCanCww1zTb5aDXL+qpU1Yhn36ENTVwg4lwte\n" +
            "7yixL8BNWy59F9Tgpl7OsPovwA6w5qetCmSd4BYfp1Wc0UkRNVeGwxNOR5LVfxWR\n" +
            "osrhGfqbNwLhcDtXLwgeELD1Ru/XdCoKGwJ47caciyLjCBf4lbo/yOpj1593nea6\n" +
            "qqtG/n9ueO54PFsfRQOIxvuoqN32CX2qzUGUd5OHfnyCrUQgCEyi9+sD1pXSgQxW\n" +
            "lTz16QKCAQEAnDz01Hoadt62Yk6JBxXVb+GSa5lgZ8B2qF8JjpqGpzHAkqBUafQ1\n" +
            "sA/tJziWE+z1s0QFT9y/55HF2sJlWgul7tSfi9f2VRbMxn3NhfilBCRCJa7AzXOy\n" +
            "RT7J2HnQCVGdoYi28wzYkS1lSIixQPQTN/cl7TwRhmYpavYJm2lpxywyhNUWHd/a\n" +
            "qlmCNHxtvQ/u5/jXfpw6dhgx1sSAtCBgmqCktvstysDK8vllVLE4heRpxUK77nzk\n" +
            "2WdywUDtf0qlKffEZMgM2FBCnIqUL6msu7AV54ppFeDbYKagEM6W1Xffc+oN5U4h\n" +
            "747lIvhOO1szD0gEBsmZIQBOUpyxig/IAQKCAQEAx3t2uaakC6oM/6M2W/BvNWwC\n" +
            "EMftSPX56fXqFrogqQ/6TMb2OJZBJ3vtQ9AmWFf0DtCTD9xhu/w1O2m21ZfrNEWh\n" +
            "T/E1Wq+7rBw2tyYSaAn3w2Nz0xyCGzZtEbqwj5FxG5CImW1Fp18fty1pKasq1nkG\n" +
            "voOYfZZlv38LK1UYvr4gjN7ZvUT4FdtC0+L8PcMbLicgBC7bm8tbpcFcgnylHm0k\n" +
            "lLcAXAgTZyHUbyuvGvjvqabu4hYGJxjcEZstZLnXrfdnsUgV25AGAFRZlRkD4gvQ\n" +
            "WT1o1GbWZd05dyZ9TVstUIU+RMRGXLl/QalBm8M7BiujcQn7/24e6g8o4363Rw==\n" +
            "-----END RSA PRIVATE KEY-----\n"

    const val PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n" +
            "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAz0iqJpro3ojTifm3pXo1\n" +
            "AYQ/3f3AV5fwmpZU0vJzjxFkZ4jZfiKcxbOjUB5duDUxPCzifX81DWkHlX+90wwh\n" +
            "Qc40HBP37odOwuMM782I55YmgF44d+svBzUWxJ9pWglYuqI2X9gtZg15E+Cr+dee\n" +
            "N7Br/xku93vsv9u4BoANo8gT20p3Uzp6U0X4ixNWPnCBPvj+iaF69qMT4knPRl8O\n" +
            "B9QgIYpzSxA4FblgONx121ijaVUzZL+wSl8M+vtHxJD6ZTwvGXlAuBsK7Y2fu9V8\n" +
            "p1NOmxoZJCKLGLGtny9eAd0L++CrOZ6ViXfwoz29n878t4ZdFEm3qh3Zo60Mvpux\n" +
            "iFtj4SVq0rx3FEjCZU8gGZH1VJUsWLWipq2L+khrf0MpspsNDjmE1bgHG5jES6Aa\n" +
            "frVa6mwVeE7gbGCLXyKH+BCPe4TnjBNlX4ryR5t58w13uQSS4wedNxPsBj+P70Va\n" +
            "hVKna3HC5NHds60YJ1ME4CJJfJxLyPvJwPUWn08Ct2iNaqs56+cgVNR0MdanHUl5\n" +
            "XBU6v6nHWZtYPau243MimQr6YU08wWVrXBnLkxBjLBqyAWg4UMnL++rqqcRIiPDi\n" +
            "gOSf5ASXXlaDvDdVASX73KVTX3ZHQUgPjoS0XG5Dm4x1qs9Ey2ROmSxaojsx3EmM\n" +
            "vDFkf7GzJ7U7PQ6nb9/3JJkCAwEAAQ==\n" +
            "-----END PUBLIC KEY-----\n"
}
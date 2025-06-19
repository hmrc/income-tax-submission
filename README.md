
# income-tax-submission

This repository is for orchestrating session setup to gather a customer's data from the relevant domain microservices

## Running the service locally

You will need to have the following:

- Installed/configured [service manager v2](https://github.com/hmrc/sm2).

This can be found in the [developer handbook](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/)

The service manager profile for this service is:

    sm2 --start INCOME_TAX_SUBMISSION

Run the following command to start the remaining services locally:

    sm2 --start INCOME_TAX_SUBMISSION_ALL

This service runs on port:  `localhost:9304`

To test the branch you're working on locally. You will need to run `sm2 --stop INCOME_TAX_SUBMISSION` followed by
`./run.sh`

### Running Tests

- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it/test`
- Run Unit and Integration Tests: `sbt test it/test`
- Run Unit and Integration Tests with coverage report: `./check.sh`<br/>
  which runs `sbt clean coverage test it/test coverageReport dependencyUpdates`

### Feature Switches

| Feature                       | Description                     |
|-------------------------------|---------------------------------|
| selfEmploymentTaskListEnabled | Doesn't do anything in the code |

### income-tax-submission endpoints

This repository retrieves data from backends relevant to a particular income source. All the endpoints follow a similar format, which are listed below:

- **GET /income-tax-dividends/income-tax/nino/:nino/sources?taxYear=:taxYear** (Gets dividends data for a particular nino and tax year)
- **GET /income-tax-interest/income-tax/nino/:nino/sources?taxYear=:taxYear** (Gets interest data for a particular nino and tax year)
- **GET /income-tax-gift-aid/income-tax/nino/:nino/sources?taxYear=:taxYear** (Gets charity data for a particular nino and tax year)
- **GET /income-tax-cis/income-tax/nino/:nino/sources?taxYear=:taxYear** (Gets cis data for a particular nino and tax year)
- **GET /income-tax-employment/income-tax/nino/:nino/sources?taxYear=:taxYear** (Gets employment data for a particular nino and tax year)
- **GET /income-tax-pensions/income-tax/nino/:nino/sources?taxYear=:taxYear** (Gets pensions data for a particular nino and tax year)

### connected microservices

We currently connect to six different microservices in order to retrieve data for a particular income source.

For more information on the data retrieved from each of the income sources click on the links below:

- [income-tax-dividends](https://github.com/hmrc/income-tax-dividends/blob/main/README.md)
- [income-tax-interest](https://github.com/hmrc/income-tax-interest/blob/main/README.md)
- [income-tax-gift-aid](https://github.com/hmrc/income-tax-gift-aid/blob/main/README.md) 
- [income-tax-cis](https://github.com/hmrc/income-tax-cis/blob/main/README.md)
- [income-tax-employment](https://github.com/hmrc/income-tax-employment/blob/main/README.md)
- [income-tax-pensions](https://github.com/hmrc/income-tax-pensions/blob/main/README.md)

## Ninos with stubbed data for income-tax-submission

### In-Year

| Nino      | Income sources data                                              | Source     |
|-----------|------------------------------------------------------------------|------------|
| AA000001A | Dividends user                                                   |            |
| AA000003A | Interest and dividends data                                      |            |
| AA123459A | All income sources user                                          | HMRC-Held  |
| AA637489D | Gift aid user                                                    |            |
| AA133742A | Single employment - Employment details, benefits and expenses    | HMRC-Held  |
| BB444444A | Multiple employments - Employment details, benefits and expenses | HMRC-Held  |
| AA370773A | Multiple employments - `occPen` set to true                      | HMRC-Held  |
| AC150000B | CIS User with multiple CIS deductions                            | Contractor |
| AA370343B | User with pension reliefs, pension charges and state benefits    |            |

### End of Year

| Nino      | Income sources data                                              | Source               |
|-----------|------------------------------------------------------------------|----------------------|
| AA123459A | User with data for all income sources                            | HMRC-Held, Customer  |
| AA133742A | Single employment - Employment details and benefits              | HMRC-Held, Customer  |
| BB444444A | Multiple employments - Employment details, benefits and expenses | HMRC-Held, Customer  |
| AA370773A | Multiple employments - `occPen` set to true                      | HMRC-Held, Customer  |
| AA455555A | User with ignored hmrc data (Employments can be reinstated)      | HMRC-Held            |
| AA333444A | User with only expenses data                                     | HMRC-Held            |
| AC150000B | CIS User with multiple CIS deductions                            | Contractor, Customer |
| AA370343B | User with pension reliefs, pension charges and state benefits    |                      |

<details>
<summary>Click here to see an example of a user with previously submitted data(JSON)</summary>

```json
{
  "dividends": {
    "ukDividends": 99999999999.99
  },
  "interest": [
    {
      "accountName": "Rick Owens Bank",
      "incomeSourceId": "000000000000001",
      "taxedUkInterest": 99999999999.99,
      "untaxedUkInterest": 99999999999.99
    },
    {
      "accountName": "Rick Owens Taxed Bank",
      "incomeSourceId": "000000000000002",
      "taxedUkInterest": 99999999999.99
    },
    {
      "accountName": "Rick Owens Untaxed Bank",
      "incomeSourceId": "000000000000003",
      "untaxedUkInterest": 99999999999.99
    }
  ],
  "giftAid": {
    "giftAidPayments": {
      "nonUkCharitiesCharityNames": [
        "Rick Owens Charity"
      ],
      "currentYear": 99999999999.99,
      "oneOffCurrentYear": 99999999999.99,
      "currentYearTreatedAsPreviousYear": 99999999999.99,
      "nextYearTreatedAsCurrentYear": 99999999999.99,
      "nonUkCharities": 99999999999.99
    },
    "gifts": {
      "investmentsNonUkCharitiesCharityNames": [
        "Rick Owens Non-UK Charity"
      ],
      "landAndBuildings": 99999999999.99,
      "sharesOrSecurities": 99999999999.99,
      "investmentsNonUkCharities": 99999999999.99
    }
  },
  "employment": {
    "hmrcEmploymentData": [
      {
        "employmentId": "00000000-0000-0000-0000-000000000001",
        "employerName": "Rick Owens Milan LTD",
        "employerRef": "666/66666",
        "payrollId": "123456789",
        "startDate": "2020-01-04",
        "cessationDate": "2020-01-04",
        "employmentData": {
          "submittedOn": "2020-01-04T05:01:01Z",
          "pay": {
            "taxablePayToDate": 666.66,
            "totalTaxToDate": 666.66,
            "payFrequency": "CALENDAR MONTHLY",
            "paymentDate": "2020-04-23",
            "taxWeekNo": 32
          }
        }
      }
    ],
    "hmrcExpenses": {
      "submittedOn": "2022-12-12T12:12:12Z",
      "totalExpenses": 100,
      "expenses": {
        "businessTravelCosts": 100,
        "jobExpenses": 100,
        "flatRateJobExpenses": 100,
        "professionalSubscriptions": 100,
        "hotelAndMealExpenses": 100,
        "otherAndCapitalAllowances": 100,
        "vehicleExpenses": 100,
        "mileageAllowanceRelief": 100
      }
    },
    "customerEmploymentData": [
      {
        "employmentId": "00000000-0000-0000-0000-000000000002",
        "employerName": "Rick Owens London LTD",
        "employerRef": "666/66666",
        "payrollId": "123456789",
        "startDate": "2020-02-04",
        "cessationDate": "2020-02-04",
        "submittedOn": "2020-02-04T05:01:01Z",
        "employmentData": {
          "submittedOn": "2020-02-04T05:01:01Z",
          "pay": {
            "taxablePayToDate": 555.55,
            "totalTaxToDate": 555.55,
            "payFrequency": "CALENDAR MONTHLY",
            "paymentDate": "2020-04-23",
            "taxWeekNo": 32
          }
        }
      }
    ]
  },
  "pensions": [
  {
          "taxYear": 2023,
          "pensionReliefs": {
              "submittedOn": "2020-07-27T17:00:19Z",
              "pensionReliefs": {
                  "regularPensionContributions": 50,
                  "oneOffPensionContributionsPaid": 170,
                  "retirementAnnuityPayments": 180,
                  "paymentToEmployersSchemeNoTaxRelief": 60,
                  "overseasPensionSchemeContributions": 40
              }
          },
          "pensionCharges": {
              "submittedOn": "2020-07-27T17:00:19Z",
              "pensionSavingsTaxCharges": {
                  "pensionSchemeTaxReference": ["00123456RA", "00123456RB"],
                  "lumpSumBenefitTakenInExcessOfLifetimeAllowance": {
                      "amount": 800.02,
                      "taxPaid": 200.02
                  },
                  "benefitInExcessOfLifetimeAllowance": {
                      "amount": 800.02,
                      "taxPaid": 200.02
                  },
                  "isAnnualAllowanceReduced": false,
                  "taperedAnnualAllowance": false,
                  "moneyPurchasedAllowance": false
              },
              "pensionSchemeOverseasTransfers": {
                  "overseasSchemeProvider": [
                {
                      "providerName": "overseas providerName 1 qualifying scheme",
                      "providerAddress": "overseas address 1",
                      "providerCountryCode": "ESP",
                      "qualifyingRecognisedOverseasPensionScheme": ["Q100000", "Q100002"]
                  }
                ],
                  "transferCharge": 123.45,
                  "transferChargeTaxPaid": 0
              },
              "pensionSchemeUnauthorisedPayments": {
                  "pensionSchemeTaxReference": [
                    "00123456RA", "00123456RB"
                  ],
                  "surcharge": {
                      "amount": 124.44,
                      "foreignTaxPaid": 123.33
                  },
                  "noSurcharge": {
                      "amount": 222.44,
                      "foreignTaxPaid": 223.33
                  }
              },
              "pensionContributions": {
                  "pensionSchemeTaxReference": [
                  "00123456RA", "00123456RB"
                  ],
                  "inExcessOfTheAnnualAllowance": 150.67,
                  "annualAllowanceTaxPaid": 178.65
              },
              "overseasPensionContributions": {
                  "overseasSchemeProvider": [
                    {
                      "providerName": "overseas providerName 1 tax ref",
                      "providerAddress": "overseas address 1",
                      "providerCountryCode": "ESP",
                      "pensionSchemeTaxReference": [
                      "00123456RA", "00123456RB"
                    ]
                  }
              ],
                  "shortServiceRefund": 1.11,
                  "shortServiceRefundTaxPaid": 2.22
              }
          },
          "stateBenefits": {
              "stateBenefits": {
                  "incapacityBenefit": [
                    {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
                      "startDate": "2019-11-13",
                      "dateIgnored": "2019-04-11T16:22:00Z",
                      "submittedOn": "2020-09-11T17:23:00Z",
                      "endDate": "2020-08-23",
                      "amount": 1212.34,
                      "taxPaid": 22323.23
                    }
                  ],
                  "statePension": {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c935",
                      "startDate": "2018-06-03",
                      "dateIgnored": "2018-09-09T19:23:00Z",
                      "submittedOn": "2020-08-07T12:23:00Z",
                      "endDate": "2020-09-13",
                      "amount": 42323.23,
                      "taxPaid": 2323.44
                  },
                  "statePensionLumpSum": {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
                      "startDate": "2019-04-23",
                      "dateIgnored": "2019-07-08T05:23:00Z",
                      "submittedOn": "2020-03-13T19:23:00Z",
                      "endDate": "2020-08-13",
                      "amount": 45454.23,
                      "taxPaid": 45432.56
                  },
                  "employmentSupportAllowance": [
                    {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c937",
                      "startDate": "2019-09-23",
                      "dateIgnored": "2019-09-28T10:23:00Z",
                      "submittedOn": "2020-11-13T19:23:00Z",
                      "endDate": "2020-08-23",
                      "amount": 44545.43,
                      "taxPaid": 35343.23
                    }
                  ],
                  "jobSeekersAllowance": [
                    {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c938",
                      "startDate": "2019-09-19",
                      "dateIgnored": "2019-08-18T13:23:00Z",
                      "submittedOn": "2020-07-10T18:23:00Z",
                      "endDate": "2020-09-23",
                      "amount": 33223.12,
                      "taxPaid": 44224.56
                    }
                  ],
                  "bereavementAllowance": {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c939",
                      "startDate": "2019-05-22",
                      "dateIgnored": "2020-08-10T12:23:00Z",
                      "submittedOn": "2020-09-19T19:23:00Z",
                      "endDate": "2020-09-26",
                      "amount": 56534.23,
                      "taxPaid": 34343.57
                  },
                  "otherStateBenefits": {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c940",
                      "startDate": "2018-09-03",
                      "dateIgnored": "2020-01-11T15:23:00Z",
                      "submittedOn": "2020-09-13T15:23:00Z",
                      "endDate": "2020-06-03",
                      "amount": 56532.45,
                      "taxPaid": 5656.89
                  }
              },
              "customerAddedStateBenefits": {
                  "incapacityBenefit": [
                    {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
                      "startDate": "2018-07-17",
                      "submittedOn": "2020-11-17T19:23:00Z",
                      "endDate": "2020-09-23",
                      "amount": 45646.78,
                      "taxPaid": 4544.34
                    }
                  ],
                  "statePension": {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c943",
                      "startDate": "2018-04-03",
                      "submittedOn": "2020-06-11T10:23:00Z",
                      "endDate": "2020-09-13",
                      "amount": 45642.45,
                      "taxPaid": 6764.34
                  },
                  "statePensionLumpSum": {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c956",
                      "startDate": "2019-09-23",
                      "submittedOn": "2020-06-13T05:29:00Z",
                      "endDate": "2020-09-26",
                      "amount": 34322.34,
                      "taxPaid": 4564.45
                  },
                  "employmentSupportAllowance": [
                    {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c988",
                      "startDate": "2019-09-11",
                      "submittedOn": "2020-02-10T11:20:00Z",
                      "endDate": "2020-06-13",
                      "amount": 45424.23,
                      "taxPaid": 23232.34
                    }
                  ],
                  "jobSeekersAllowance": [
                    {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c990",
                      "startDate": "2019-07-10",
                      "submittedOn": "2020-05-13T14:23:00Z",
                      "endDate": "2020-05-11",
                      "amount": 34343.78,
                      "taxPaid": 3433.56
                    } 
                  ],
                  "bereavementAllowance": {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c997",
                      "startDate": "2018-08-12",
                      "submittedOn": "2020-02-13T11:23:00Z",
                      "endDate": "2020-07-13",
                      "amount": 45423.45,
                      "taxPaid": 4543.64
                  },
                  "otherStateBenefits": {
                      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c957",
                      "startDate": "2018-01-13",
                      "submittedOn": "2020-09-12T12:23:00Z",
                      "endDate": "2020-08-13",
                      "amount": 63333.33,
                      "taxPaid": 4644.45
                  }
              }
          }
      }
  ],
  "cis": [
    {
        "taxYear": 2023,
        "customerCISDeductions": {
            "totalDeductionAmount": 400,
            "totalCostOfMaterials": 400,
            "totalGrossAmountPaid": 400,
            "cisDeductions": [
              {
                "fromDate": "2021-04-06",
                "toDate": "2022-04-05",
                "contractorName": "Michele Lamy Paving Ltd",
                "employerRef": "111/11111",
                "totalDeductionAmount": 200,
                "totalCostOfMaterials": 200,
                "totalGrossAmountPaid": 200,
                "periodData": [
                  {
                    "deductionFromDate": "2021-04-06",
                    "deductionToDate": "2021-05-05",
                    "deductionAmount": 100,
                    "costOfMaterials": 100,
                    "grossAmountPaid": 100,
                    "submissionDate": "2022-05-11T16:38:57.489Z",
                    "submissionId": "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                    "source": "customer"
                  }, {
                    "deductionFromDate": "2021-05-06",
                    "deductionToDate": "2021-06-05",
                    "deductionAmount": 100,
                    "costOfMaterials": 100,
                    "grossAmountPaid": 100,
                    "submissionDate": "2022-05-11T16:38:57.489Z",
                    "submissionId": "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                    "source": "customer"
                  }
                ]
              }, {
                "fromDate": "2021-04-06",
                "toDate": "2022-04-05",
                "contractorName": "Jun Takahashi Window Fitting",
                "employerRef": "222/11111",
                "totalDeductionAmount": 200,
                "totalCostOfMaterials": 200,
                "totalGrossAmountPaid": 200,
                "periodData": [
                  {
                    "deductionFromDate": "2021-04-06",
                    "deductionToDate": "2021-05-05",
                    "deductionAmount": 100,
                    "costOfMaterials": 100,
                    "grossAmountPaid": 100,
                    "submissionDate": "2022-05-11T16:38:57.489Z",
                    "submissionId": "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                    "source": "customer"
                  }, {
                    "deductionFromDate": "2021-05-06",
                    "deductionToDate": "2021-06-05",
                    "deductionAmount": 100,
                    "costOfMaterials": 100,
                    "grossAmountPaid": 100,
                    "submissionDate": "2022-05-11T16:38:57.489Z",
                    "submissionId": "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                    "source": "customer"
                  }
                ]
              }
            ]
        },
        "contractorCISDeductions": {
            "totalDeductionAmount": 400,
            "totalCostOfMaterials": 400,
            "totalGrossAmountPaid": 400,
            "cisDeductions": [
              {
                "fromDate": "2021-04-06",
                "toDate": "2022-04-05",
                "contractorName": "Michele Lamy Paving Ltd",
                "employerRef": "111/11111",
                "totalDeductionAmount": 200,
                "totalCostOfMaterials": 200,
                "totalGrossAmountPaid": 200,
                "periodData": [
                  {
                    "deductionFromDate": "2021-04-06",
                    "deductionToDate": "2021-05-05",
                    "deductionAmount": 100,
                    "costOfMaterials": 100,
                    "grossAmountPaid": 100,
                    "submissionDate": "2022-05-11T16:38:57.489Z",
                    "source": "contractor"
                  }, {
                    "deductionFromDate": "2021-05-06",
                    "deductionToDate": "2021-06-05",
                    "deductionAmount": 100,
                    "costOfMaterials": 100,
                    "grossAmountPaid": 100,
                    "submissionDate": "2022-05-11T16:38:57.489Z",
                    "source": "contractor"
                  }
                ]
              }, {
                "fromDate": "2021-04-06",
                "toDate": "2022-04-05",
                "contractorName": "Jun Takahashi Window Fitting",
                "employerRef": "222/11111",
                "totalDeductionAmount": 200,
                "totalCostOfMaterials": 200,
                "totalGrossAmountPaid": 200,
                "periodData": [
                  {
                    "deductionFromDate": "2021-04-06",
                    "deductionToDate": "2021-05-05",
                    "deductionAmount": 100,
                    "costOfMaterials": 100,
                    "grossAmountPaid": 100,
                    "submissionDate": "2022-05-11T16:38:57.489Z",
                    "source": "contractor"
                  }, {
                    "deductionFromDate": "2021-05-06",
                    "deductionToDate": "2021-06-05",
                    "deductionAmount": 100,
                    "costOfMaterials": 100,
                    "grossAmountPaid": 100,
                    "submissionDate": "2022-05-11T16:38:57.489Z",
                    "source": "contractor"
                  }
                ]
              }
            ]
        }
    }
  ]
}
```
</details>

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

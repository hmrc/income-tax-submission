
# income-tax-submission

This repository is for orchestrating session setup to gather a customer's data from the relevant domain microservices

## Running the service locally

You will need to have the following:

- Installed/configured [service manager v2](https://github.com/hmrc/sm2).

The service manager profile for this service is:

    sm2 --start INCOME_TAX_SUBMISSION

Run the following command to start the remaining services locally:

    sudo mongodb (If not already running)
    sm2 --start INCOME_TAX_SUBMISSION_ALL -r

This service runs on port:  `localhost:9304`

### Running Tests

- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it/test`
- Run Unit and Integration Tests: `sbt test it/test`
- Run Unit and Integration Tests with coverage report: `sbt runAllChecks`<br/>
  which runs `clean compile coverage test it/test coverageReport dependencyUpdates`

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

| Nino      | Income sources data                                             | Source     |
|-----------|-----------------------------------------------------------------|------------|
| AA000001A | Dividends user                                                  |            |
| AA000003A | Interest and dividends data                                     |            |
| AA123459A | All income sources user                                         | HMRC-Held  |
| AA637489D | Gift aid user                                                   |            |
| AA133742A | Single employment - Employment details, benefits and expenses    | HMRC-Held  |
| BB444444A | Multiple employments - Employment details, benefits and expenses | HMRC-Held  |
| AA370773A | Multiple employments - `occPen` set to true                     | HMRC-Held  |
| AC150000B | CIS User with multiple CIS deductions                           | Contractor |
| AA370343B | User with pension reliefs, pension charges and state benefits    |            |

### End of Year

| Nino      | Income sources data                                             | Source               |
|-----------|-----------------------------------------------------------------|----------------------|
| AA123459A | User with data for all income sources                           | HMRC-Held, Customer  |
| AA133742A | Single employment - Employment details and benefits              | HMRC-Held, Customer  |
| BB444444A | Multiple employments - Employment details, benefits and expenses | HMRC-Held, Customer  |
| AA370773A | Multiple employments - `occPen` set to true                     | HMRC-Held, Customer  |
| AA455555A | User with ignored hmrc data (Employments can be reinstated)     | HMRC-Held            |
| AA333444A | User with only expenses data                                    | HMRC-Held            |
| AC150000B | CIS User with multiple CIS deductions                           | Contractor, Customer |
| AA370343B | User with pension reliefs, pension charges and state benefits    |                      |


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

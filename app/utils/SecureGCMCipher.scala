/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package utils

import java.security.{InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, SecureRandom}
import java.util.Base64

import config.AppConfig
import javax.crypto.spec.{GCMParameterSpec, SecretKeySpec}
import javax.crypto._
import javax.inject.{Inject, Singleton}
import models._
import models.employment.frontend.{AllEmploymentData, EncryptedAllEmploymentData}
import models.giftAid.{EncryptedGiftAidModel, EncryptedGiftAidPaymentsModel, EncryptedGiftsModel, GiftAidModel, GiftAidPaymentsModel, GiftsModel}
import models.mongo.{EncryptedUserData, UserData}
import play.api.libs.json.{Json, OFormat}
import repositories.IncomeTaxUserDataRepository

import scala.util.{Failure, Success, Try}


case class EncryptedValue(value: String, nonce: String)

object EncryptedValue {
  implicit val cryptoFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
}

class EncryptionDecryptionException(method: String, reason: String, message: String) extends RuntimeException {
  val failureReason = s"$reason for $method"
  val failureMessage: String = message
}

@Singleton
class SecureGCMCipher @Inject()(implicit private val appConfig: AppConfig) {

  val IV_SIZE = 96
  val TAG_BIT_LENGTH = 128
  val ALGORITHM_TO_TRANSFORM_STRING = "AES/GCM/PKCS5Padding"
  lazy val secureRandom = new SecureRandom()
  val ALGORITHM_KEY = "AES"
  val METHOD_ENCRYPT = "encrypt"
  val METHOD_DECRYPT = "decrypt"

//  def encryptBankDetails(bankDetails: BankDetails, employerRef: String, key: String): EncryptedBankDetails = {
//    def encryptValues(bd: BankDetails, employerRef: String, key: String): EncryptedBankDetails = {
//      def e(field: String): EncryptedValue = encrypt(field, employerRef, key)
//      val (account, sort, name, address, payroll, bankName) = (
//        bd.accountNumber, bd.sortCode, bd.nameOnAccount, bd.address, bd.buildingSocietyRollNumber, bd.bankName
//      )
//
//      EncryptedBankDetails(e(account), e(sort), name map e, address, payroll.map(e), bankName)
//    }
//
//    encryptValues(bankDetails, employerRef, key)
//  }
//
//  def encryptContactDetails(contactDetails: ContactDetails, employerRef: String, key: String): EncryptedContactDetails = {
//    def encryptValues(cd: ContactDetails, employerRef: String, key: String): EncryptedContactDetails = {
//      def e(field: String): EncryptedValue = encrypt(field, employerRef, key)
//      val (name, number) = (cd.name, cd.number)
//      EncryptedContactDetails(e(name), e(number))
//    }
//
//    encryptValues(contactDetails, employerRef, key)
//  }
//
//  def decryptClaim(claim: Claim, employerRef: String, key: String): (String, UserSubmission) = {
//
//    def d(field: EncryptedValue): String = decrypt(field.value, field.nonce, employerRef, key)
//
//    val (account, sort, accountName, address, payroll, bankName) = (claim.bankDetails.accountNumber, claim.bankDetails.sortCode,
//                                                          claim.bankDetails.nameOnAccount, claim.bankDetails.address,
//                                                          claim.bankDetails.buildingSocietyRollNumber, claim.bankDetails.bankName)
//
//    val bankDetails = BankDetails(d(account), d(sort), accountName map d, address, payroll.map(d), bankName)
//    val (name, number) = (claim.contactDetails.name, claim.contactDetails.number)
//    val contactDetails = ContactDetails(d(name), d(number))
//
//    val cd = claim.claimDetails
//
//    val (start, end, numEmployees, total, disclaimer, employLessThan250, employeesAlreadyPaid, confirmedClaimAmount) = (
//      cd.startDate, cd.endDate, cd.numberOfEmployees, cd.claimAmount, cd.disclaimerAccepted, cd.employLessThan250,
//      cd.employeesAlreadyPaid, cd.confirmedClaimAmount
//    )
//
//    val claimDetails = ClaimDetails(start, end, numEmployees, total, disclaimer, employLessThan250, employeesAlreadyPaid, confirmedClaimAmount)
//
//    claim.claimId -> UserSubmission(claimDetails, bankDetails, contactDetails)
//  }

  def encryptUserData(userData: UserData): EncryptedUserData ={
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId,appConfig.encryptionKey)

    EncryptedUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      dividends = userData.dividends.map(encryptDividends),
      interest = userData.interest.map(encryptInterest),
      giftAid = userData.giftAid.map(encryptGiftAid),
      employment = userData.employment.map(encryptEmployment),
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptDividends(dividends: DividendsModel)(implicit textAndKey: TextAndKey): EncryptedDividendsModel ={
    EncryptedDividendsModel(
      ukDividends = dividends.ukDividends.map(encrypt[BigDecimal]),
      otherUkDividends = dividends.otherUkDividends.map(encrypt[BigDecimal])
    )
  }

  private def encryptInterest(interest: Seq[InterestModel])(implicit textAndKey: TextAndKey): Seq[EncryptedInterestModel] ={
    interest.map{
      interest =>
        EncryptedInterestModel(
          accountName = encrypt[String](interest.accountName),
          incomeSourceId = encrypt[String](interest.incomeSourceId),
          taxedUkInterest = interest.taxedUkInterest.map(encrypt[BigDecimal]),
          untaxedUkInterest = interest.untaxedUkInterest.map(encrypt[BigDecimal])
        )
    }
  }

  private def encryptGiftAid(giftAid: GiftAidModel)(implicit textAndKey: TextAndKey): EncryptedGiftAidModel ={

    val eGiftAidPayments: Option[EncryptedGiftAidPaymentsModel] = {
      giftAid.giftAidPayments.map{
        g =>
          EncryptedGiftAidPaymentsModel(
            nonUkCharitiesCharityNames = g.nonUkCharitiesCharityNames.map(_.map(encrypt)),
            currentYear = g.currentYear.map(encrypt),
            oneOffCurrentYear = g.oneOffCurrentYear.map(encrypt),
            currentYearTreatedAsPreviousYear = g.currentYearTreatedAsPreviousYear.map(encrypt),
            nextYearTreatedAsCurrentYear = g.nextYearTreatedAsCurrentYear.map(encrypt),
            nonUkCharities = g.nonUkCharities.map(encrypt)
          )
      }
    }
    val eGifts: Option[EncryptedGiftsModel] = {
      giftAid.gifts.map {
        g =>
          EncryptedGiftsModel(
            investmentsNonUkCharitiesCharityNames = g.investmentsNonUkCharitiesCharityNames.map(_.map(encrypt)),
            landAndBuildings = g.landAndBuildings.map(encrypt),
            sharesOrSecurities = g.sharesOrSecurities.map(encrypt),
            investmentsNonUkCharities = g.investmentsNonUkCharities.map(encrypt)
          )
      }
    }

    EncryptedGiftAidModel(giftAidPayments = eGiftAidPayments, gifts = eGifts)
  }

  private def encryptEmployment(employment: AllEmploymentData)(implicit textAndKey: TextAndKey): EncryptedAllEmploymentData ={
    ???
  }

  def decryptUserData(userData: EncryptedUserData)(implicit textAndKey: TextAndKey): UserData ={
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId,appConfig.encryptionKey)

    UserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      dividends = userData.dividends.map(decryptDividends),
      interest = userData.interest.map(decryptInterest),
      giftAid = userData.giftAid.map(decryptGiftAid),
      employment = userData.employment.map(decryptEmployment),
      lastUpdated = userData.lastUpdated
    )
  }

  private def decryptDividends(dividends: EncryptedDividendsModel)(implicit textAndKey: TextAndKey): DividendsModel ={
    DividendsModel(
      dividends.ukDividends.map(x => decrypt[BigDecimal](x.value,x.nonce)),
      dividends.otherUkDividends.map(x => decrypt[BigDecimal](x.value,x.nonce))
    )
  }

  private def decryptInterest(interest: Seq[EncryptedInterestModel])(implicit textAndKey: TextAndKey): Seq[InterestModel] ={
    interest.map{
      interest =>
        InterestModel(
          accountName = decrypt[String](interest.accountName.value,interest.accountName.nonce),
          incomeSourceId = decrypt[String](interest.incomeSourceId.value,interest.incomeSourceId.nonce),
          taxedUkInterest = interest.taxedUkInterest.map(x => decrypt[BigDecimal](x.value,x.nonce)),
          untaxedUkInterest = interest.untaxedUkInterest.map(x => decrypt[BigDecimal](x.value,x.nonce))
        )
    }
  }

  private def decryptGiftAid(giftAid: EncryptedGiftAidModel)(implicit textAndKey: TextAndKey): GiftAidModel ={

    val dGiftAidPayments: Option[GiftAidPaymentsModel] = {
      giftAid.giftAidPayments.map{
        g =>
          GiftAidPaymentsModel(
            nonUkCharitiesCharityNames = g.nonUkCharitiesCharityNames.map(_.map(x => decrypt(x.value,x.nonce))),
            currentYear = g.currentYear.map(x => decrypt(x.value,x.nonce)),
            oneOffCurrentYear = g.oneOffCurrentYear.map(x => decrypt(x.value,x.nonce)),
            currentYearTreatedAsPreviousYear = g.currentYearTreatedAsPreviousYear.map(x => decrypt(x.value,x.nonce)),
            nextYearTreatedAsCurrentYear = g.nextYearTreatedAsCurrentYear.map(x => decrypt(x.value,x.nonce)),
            nonUkCharities = g.nonUkCharities.map(x => decrypt(x.value,x.nonce))
          )
      }
    }

    val dGifts: Option[GiftsModel] = {
      giftAid.gifts.map {
        g =>
          GiftsModel(
            investmentsNonUkCharitiesCharityNames = g.investmentsNonUkCharitiesCharityNames.map(_.map(x => decrypt(x.value,x.nonce))),
            landAndBuildings = g.landAndBuildings.map(x => decrypt(x.value,x.nonce)),
            sharesOrSecurities = g.sharesOrSecurities.map(x => decrypt(x.value,x.nonce)),
            investmentsNonUkCharities = g.investmentsNonUkCharities.map(x => decrypt(x.value,x.nonce))
          )
      }
    }

    GiftAidModel(giftAidPayments = dGiftAidPayments, gifts = dGifts)
  }

  private def decryptEmployment(employment: EncryptedAllEmploymentData)(implicit textAndKey: TextAndKey): AllEmploymentData={
    ???
  }

  private[utils] def getCipherInstance: Cipher = Cipher.getInstance(ALGORITHM_TO_TRANSFORM_STRING)

  case class TextAndKey(associatedText: String, aesKey: String)

  def encrypt[T](valueToEncrypt: T)(implicit textAndKey: TextAndKey): EncryptedValue = {

    val initialisationVector = generateInitialisationVector
    val nonce = new String(Base64.getEncoder.encode(initialisationVector))
    val gcmParameterSpec = new GCMParameterSpec(TAG_BIT_LENGTH, initialisationVector)
    val secretKey = validateSecretKey(textAndKey.aesKey, METHOD_ENCRYPT)
    val cipherText = generateCipherText(valueToEncrypt.toString, validateAssociatedText(textAndKey.associatedText, METHOD_ENCRYPT), gcmParameterSpec, secretKey)
    EncryptedValue(cipherText, nonce)
  }

  def decrypt[T](valueToDecrypt: String, nonce: String)(implicit textAndKey: TextAndKey): T = {
    val initialisationVector = Base64.getDecoder.decode(nonce)
    val gcmParameterSpec = new GCMParameterSpec(TAG_BIT_LENGTH, initialisationVector)
    val secretKey = validateSecretKey(textAndKey.aesKey, METHOD_DECRYPT)

    Try {
      decryptCipherText(valueToDecrypt, validateAssociatedText(textAndKey.associatedText, METHOD_DECRYPT), gcmParameterSpec, secretKey).asInstanceOf[T]
    }.toEither match {
      case Left(exception) => throw exception
      case Right(value) => value
    }
  }

  private def generateInitialisationVector: Array[Byte] = {
    val iv = new Array[Byte](IV_SIZE)
    secureRandom.nextBytes(iv)
    iv
  }

  private def validateSecretKey(key: String, method: String): SecretKey = Try {
    val decodedKey = Base64.getDecoder.decode(key)
    new SecretKeySpec(decodedKey, 0 , decodedKey.length, ALGORITHM_KEY)
  } match {
    case Success(secretKey) => secretKey
    case Failure(ex) => throw new EncryptionDecryptionException(method, "The key provided is invalid", ex.getMessage)
  }

  def generateCipherText(valueToEncrypt: String, associatedText: Array[Byte], gcmParameterSpec: GCMParameterSpec, secretKey: SecretKey): String = {
    Try {
      val cipher = getCipherInstance
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec, new SecureRandom())
      cipher.updateAAD(associatedText)
      cipher.doFinal(valueToEncrypt.getBytes)
    } match {
      case Success(cipherTextBytes) => new String(Base64.getEncoder.encode(cipherTextBytes))
      case Failure(ex) => throw processCipherTextFailure(ex, METHOD_ENCRYPT)
    }
  }

  def decryptCipherText(valueToDecrypt: String, associatedText: Array[Byte], gcmParameterSpec: GCMParameterSpec, secretKey: SecretKey): String = {
    Try {
      val cipher = getCipherInstance
      cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec, new SecureRandom())
      cipher.updateAAD(associatedText)
      cipher.doFinal(Base64.getDecoder.decode(valueToDecrypt))
    } match {
      case Success(value) => new String(value)
      case Failure(ex) => throw processCipherTextFailure(ex, METHOD_DECRYPT)
    }
  }

  private def validateAssociatedText(associatedText: String, method: String): Array[Byte] = {
    associatedText match {
      case text if text.length > 0 => text.getBytes
      case _ => throw new EncryptionDecryptionException(method, "associated text must not be null", "associated text was not defined")
    }
  }

  private def processCipherTextFailure(ex: Throwable, method: String): Throwable = ex match {
    case e: NoSuchAlgorithmException => throw new EncryptionDecryptionException(method, "Algorithm being requested is not available in this environment",
      e.getMessage)
    case e: NoSuchPaddingException => throw new EncryptionDecryptionException(method, "Padding Scheme being requested is not available this environment",
      e.getMessage)
    case e: InvalidKeyException => throw new EncryptionDecryptionException(method, "Key being used is not valid." +
      " It could be due to invalid encoding, wrong length or uninitialized", e.getMessage)
    case e: InvalidAlgorithmParameterException => throw new EncryptionDecryptionException(method, "Algorithm parameters being specified are not valid",
      e.getMessage)
    case e: IllegalStateException => throw new EncryptionDecryptionException(method, "Cipher is in an illegal state", e.getMessage)
    case e: UnsupportedOperationException => throw new EncryptionDecryptionException(method, "Provider might not be supporting this method", e.getMessage)
    case e: IllegalBlockSizeException => throw new EncryptionDecryptionException(method, "Error occured due to block size", e.getMessage)
    case e: BadPaddingException => throw new EncryptionDecryptionException(method, "Error occured due to padding scheme", e.getMessage)
    case _ => throw new EncryptionDecryptionException(method, "Unexpected exception", ex.getMessage)
  }

}

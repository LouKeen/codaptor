package tech.b180.cordaptor.rest

import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.TransactionStorage
import net.corda.core.transactions.SignedTransaction
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import javax.json.JsonObject
import kotlin.reflect.KClass

/**
 * Serializer for [CordaX500Name] converting to/from a string value.
 */
class CordaX500NameSerializer : CustomSerializer<CordaX500Name>,
    SerializationFactory.DelegatingSerializer<CordaX500Name, String>(
    delegate = SerializationFactory.StringSerializer,
    delegate2my = { CordaX500Name.parse(it) },
    my2delegate = CordaX500Name::toString
) {
  override val appliedTo = CordaX500Name::class
}

/**
 * Serializer for [SecureHash] converting to/from a string value.
 */
class CordaSecureHashSerializer : CustomSerializer<SecureHash>,
    SerializationFactory.DelegatingSerializer<SecureHash, String>(
    delegate = SerializationFactory.StringSerializer,
    delegate2my = { SecureHash.parse(it) },
    my2delegate = SecureHash::toString
) {
  override val schema: JsonObject = mapOf(
      "type" to "string",
      "minLength" to 64,
      "maxLength" to 64,
      "pattern" to "^[A-Z0-9]{64}"
  ).asJsonObject()

  override val appliedTo = SecureHash::class
}

/**
 * Serializer for [UUID] converting to/from a string value.
 *
 * Technically it is not a Corda class, but it is commonly used in Corda API.
 */
class CordaUUIDSerializer : CustomSerializer<UUID>,
    SerializationFactory.DelegatingSerializer<UUID, String>(
    delegate = SerializationFactory.StringSerializer,
    my2delegate = UUID::toString,
    delegate2my = UUID::fromString
) {
  override val schema: JsonObject = mapOf(
      "type" to "string",
      "format" to "uuid"
  ).asJsonObject()

  override val appliedTo = UUID::class
}

class JavaInstantSerializer : CustomSerializer<Instant>,
    SerializationFactory.DelegatingSerializer<Instant, String>(
        delegate = SerializationFactory.StringSerializer,
        my2delegate = { DateTimeFormatter.ISO_INSTANT.format(this) },
        delegate2my = { Instant.parse(it) }
    ) {

  override val schema: JsonObject = mapOf(
      "type" to "string",
      "format" to "date-time"
  ).asJsonObject()

  override val appliedTo: KClass<*> = Instant::class
}

/**
 * Serializer for [Party] representing it as an object with X500 name.
 *
 * When reading from JSON, it attempts to resolve the party by calling
 * [IdentityService.wellKnownPartyFromX500Name] method
 */
class CordaPartySerializer(
    factory: SerializationFactory,
    private val identityService: IdentityService
) : CustomStructuredObjectSerializer<Party>(Party::class, factory) {

  override val properties: Map<String, ObjectProperty> = mapOf(
      "name" to KotlinObjectProperty(Party::name, isMandatory = true)
  )

  override fun initializeInstance(values: Map<String, Any?>): Party {
    val nameValue = values["name"]
    assert(nameValue is CordaX500Name) { "Expected X500 name, got $nameValue" }

    val name = nameValue as CordaX500Name

    return identityService.wellKnownPartyFromX500Name(name)
        ?: throw SerializationException("Party with name $name is not known")
  }
}

/**
 * Serializer for [SignedTransaction] representing it as JSON value.
 *
 * All transaction details are includes only in the output. For the input only transaction hash
 * is accepted, and then is used to resolve
 */
class CordaSignedTransactionSerializer(
    factory: SerializationFactory,
    private val transactionStorage: TransactionStorage
) : CustomStructuredObjectSerializer<SignedTransaction>(SignedTransaction::class, factory) {

  override val properties: Map<String, ObjectProperty> = mapOf(
      "id" to KotlinObjectProperty(SignedTransaction::id),
      "core" to KotlinObjectProperty(SignedTransaction::coreTransaction, deserialize = false)
  )

  override fun initializeInstance(values: Map<String, Any?>): SignedTransaction {
    val hashValue = values["id"]
    assert(hashValue is SecureHash) { "Expected hash, got $hashValue" }

    val hash = hashValue as SecureHash

    return transactionStorage.getTransaction(hash)
        ?: throw SerializationException("Transaction with hash $hash is not known")
  }
}

/**
 * Serializer for [PartyAndCertificate] representing it as JSON value.
 * This object is most commonly used as part of a [NodeInfo] structure.
 *
 * FIXME implement serialization logic for instances of [X509Certificate] abstract class
 */
class CordaPartyAndCertificateSerializer(private val factory: SerializationFactory)
  : CustomStructuredObjectSerializer<PartyAndCertificate>(PartyAndCertificate::class, factory, deserialize = false) {

  override val properties = mapOf(
      "party" to KotlinObjectProperty(PartyAndCertificate::party)
  )
}
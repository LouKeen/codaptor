package tech.b180.cordaptor.cordapp

import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.TransactionStorage
import net.corda.core.node.services.VaultService
import net.corda.node.services.api.ServiceHubInternal
import net.corda.serialization.internal.model.LocalTypeModel
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.b180.cordaptor.corda.CordaNodeCatalog
import tech.b180.cordaptor.corda.CordaNodeCatalogInner
import tech.b180.cordaptor.corda.CordaNodeState
import tech.b180.cordaptor.corda.CordaNodeStateInner
import tech.b180.cordaptor.kernel.ModuleProvider

/**
 * Single point of access for various APIs available within the Corda node.
 * Implementation is injected during the bootstrap process, and may be version-specific
 * where internal node APIs are being exposed.
 */
interface NodeServicesLocator {
  val appServiceHub: AppServiceHub
  val serviceHubInternal: ServiceHubInternal
  val localTypeModel: LocalTypeModel
  val transactionStorage: TransactionStorage
  val vaultService: VaultService
}

/**
 * Implementation of the microkernel module provider that makes the components
 * of this module available for injection into other modules' components.
 *
 * This class is instantiated by the microkernel at runtime using [java.util.ServiceLoader].
 */
@Suppress("UNUSED")
class CordaServiceModuleProvider : ModuleProvider {
  override val salience = ModuleProvider.INNER_MODULE_SALIENCE

  override val module = module {
    single<CordaNodeCatalog> { CordaNodeCatalogImpl(get(), get()) } bind CordaNodeCatalogInner::class
    single<CordaNodeState> { CordaNodeStateImpl() } bind CordaNodeStateInner::class
    single { CordaFlowDispatcher() }

    // expose Corda node APIs to other definitions without the need to traverse the properties
    single { (locator: NodeServicesLocator) -> locator.serviceHubInternal }
    single { (locator: NodeServicesLocator) -> locator.localTypeModel }
    single { (locator: NodeServicesLocator) -> locator.appServiceHub }
    single { (locator: NodeServicesLocator) -> locator.transactionStorage }
    single { (locator: NodeServicesLocator) -> locator.vaultService }
  }
}
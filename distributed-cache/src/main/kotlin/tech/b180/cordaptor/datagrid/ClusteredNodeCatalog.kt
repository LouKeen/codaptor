package tech.b180.cordaptor.datagrid

import org.koin.core.inject
import tech.b180.cordaptor.corda.CordaNodeCatalog
import tech.b180.cordaptor.corda.CordaNodeState
import tech.b180.cordaptor.corda.CordappInfo
import tech.b180.cordaptor.kernel.CordaptorComponent
import tech.b180.cordaptor.kernel.LifecycleAware

class ClusteredNodeCatalog : CordaNodeCatalog, CordaptorComponent, LifecycleAware {

  private val nodeCatalog by inject<CordaNodeCatalog>()
  private val nodeState by inject<CordaNodeState>()

  override val cordapps: Collection<CordappInfo>
    get() = TODO("Not yet implemented")

  override fun initialize() {
    TODO("Not yet implemented")
  }

  override fun shutdown() {
    TODO("Not yet implemented")
  }

}
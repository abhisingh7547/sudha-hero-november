package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.model.StaffRole
import com.example.data.remote.DealershipRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.ShowroomTopAppBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.SalesCrmScreen
import com.example.ui.screens.StaffHrScreen
import com.example.ui.screens.WorkshopScreen
import com.example.ui.viewmodel.DealershipViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

sealed class DealershipTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  data object Dashboard :
    DealershipTab("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)

  data object Inventory :
    DealershipTab("inventory", "Stock", Icons.Filled.Inventory2, Icons.Outlined.Inventory2)

  data object Sales :
    DealershipTab("sales", "Sales", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale)

  data object Workshop :
    DealershipTab("workshop", "Service", Icons.Filled.Build, Icons.Outlined.Build)

  data object Staff :
    DealershipTab("staff", "Staff", Icons.Filled.Groups, Icons.Outlined.Groups)
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  var currentUser by remember { mutableStateOf(auth.currentUser) }

  DisposableEffect(auth) {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      currentUser = firebaseAuth.currentUser
    }
    auth.addAuthStateListener(listener)
    onDispose { auth.removeAuthStateListener(listener) }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      onAuthSuccess = { currentUser = auth.currentUser }
    )
  } else {
    key(user.uid) {
      DealershipWorkspaceRoot(
        currentUserId = user.uid,
        displayName = user.displayName ?: "Showroom Owner",
        email = user.email ?: "",
      )
    }
  }
}

@Composable
private fun DealershipWorkspaceRoot(
  currentUserId: String,
  displayName: String,
  email: String,
) {
  val viewModel: DealershipViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) as Application
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            DealershipViewModel(
              repository = DealershipRepository(db, Firebase.auth),
              currentUserId = currentUserId,
              userDisplayName = displayName,
              userEmail = email,
            )
          }
        },
    )

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
  val metrics by viewModel.metrics.collectAsStateWithLifecycle()
  val bikesState by viewModel.bikes.collectAsStateWithLifecycle()
  val partsState by viewModel.spareParts.collectAsStateWithLifecycle()
  val staffState by viewModel.staff.collectAsStateWithLifecycle()
  val attendanceState by viewModel.attendance.collectAsStateWithLifecycle()
  val customersState by viewModel.customers.collectAsStateWithLifecycle()
  val salesState by viewModel.sales.collectAsStateWithLifecycle()
  val jobCardsState by viewModel.jobCards.collectAsStateWithLifecycle()
  val expensesState by viewModel.expenses.collectAsStateWithLifecycle()
  val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  val visibleTabs =
    remember(activeRole) {
      when (activeRole) {
        StaffRole.ADMIN ->
          listOf(
            DealershipTab.Dashboard,
            DealershipTab.Inventory,
            DealershipTab.Sales,
            DealershipTab.Workshop,
            DealershipTab.Staff,
          )
        StaffRole.SALES_EXECUTIVE ->
          listOf(
            DealershipTab.Dashboard,
            DealershipTab.Inventory,
            DealershipTab.Sales,
            DealershipTab.Staff,
          )
        StaffRole.SERVICE_MANAGER ->
          listOf(
            DealershipTab.Dashboard,
            DealershipTab.Inventory,
            DealershipTab.Workshop,
            DealershipTab.Staff,
          )
        StaffRole.MECHANIC ->
          listOf(
            DealershipTab.Workshop,
            DealershipTab.Inventory,
            DealershipTab.Staff,
          )
      }
    }

  val navController = rememberNavController()
  var currentRoute by rememberSaveable { mutableStateOf(visibleTabs.first().route) }
  var inventoryInitialSubTab by rememberSaveable { mutableIntStateOf(0) }

  LaunchedEffect(visibleTabs) {
    if (visibleTabs.none { it.route == currentRoute }) {
      val fallback = visibleTabs.first().route
      currentRoute = fallback
      navController.navigate(fallback) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }
  }

  fun navigateToTab(route: String, subTab: Int = 0) {
    if (route == DealershipTab.Inventory.route) {
      inventoryInitialSubTab = subTab
    }
    currentRoute = route
    navController.navigate(route) {
      popUpTo(navController.graph.startDestinationId) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWideScreen = maxWidth >= 600.dp

    Scaffold(
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        ShowroomTopAppBar(
          activeRole = activeRole,
          lowStockCount = metrics.lowStockPartsCount,
          onRoleSelected = viewModel::switchWorkspaceRole,
          onLowStockClick = { navigateToTab(DealershipTab.Inventory.route, subTab = 1) },
          onSignOutClick = {
            signOut(credentialManager = credentialManager, scope = scope)
          },
        )
      },
      bottomBar = {
        if (!isWideScreen) {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            visibleTabs.forEach { tab ->
              val selected = currentRoute == tab.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label, maxLines = 1) },
                modifier = Modifier.testTag("nav_${tab.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(innerPadding)
      ) {
        if (isWideScreen) {
          NavigationRail {
            visibleTabs.forEach { tab ->
              val selected = currentRoute == tab.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("rail_${tab.route}"),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = DealershipTab.Dashboard.route,
          modifier = Modifier.weight(1f).fillMaxSize(),
        ) {
          composable(DealershipTab.Dashboard.route) {
            DashboardScreen(
              metrics = metrics,
              expensesState = expensesState,
              activeRole = activeRole,
              isWideScreen = isWideScreen,
              onNavigateToInventory = { navigateToTab(DealershipTab.Inventory.route, subTab = 1) },
              onNavigateToSales = { navigateToTab(DealershipTab.Sales.route) },
              onNavigateToWorkshop = { navigateToTab(DealershipTab.Workshop.route) },
              onNavigateToStaff = { navigateToTab(DealershipTab.Staff.route) },
              onAddExpense = viewModel::addExpense,
              onDeleteExpense = viewModel::deleteExpense,
            )
          }
          composable(DealershipTab.Inventory.route) {
            InventoryScreen(
              bikesState = bikesState,
              partsState = partsState,
              initialSubTab = inventoryInitialSubTab,
              onAddBike = viewModel::addBike,
              onUpdateBikeStatus = viewModel::updateBikeStatus,
              onDeleteBike = viewModel::deleteBike,
              onAddSparePart = viewModel::addSparePart,
              onAdjustPartQty = viewModel::adjustSparePartQty,
              onDeletePart = viewModel::deleteSparePart,
            )
          }
          composable(DealershipTab.Sales.route) {
            SalesCrmScreen(
              salesState = salesState,
              customersState = customersState,
              bikesState = bikesState,
              staffState = staffState,
              onRecordSale = viewModel::recordBikeSale,
              onUpdateSaleCompliance = viewModel::updateSaleCompliance,
              onAddCustomerLead = viewModel::addCustomerLead,
              onUpdateLeadStatus = viewModel::updateCustomerLeadStatus,
            )
          }
          composable(DealershipTab.Workshop.route) {
            WorkshopScreen(
              jobCardsState = jobCardsState,
              staffState = staffState,
              onCreateJobCard = viewModel::createJobCard,
              onUpdateJobStatus = viewModel::updateJobCardStatus,
            )
          }
          composable(DealershipTab.Staff.route) {
            StaffHrScreen(
              staffState = staffState,
              attendanceState = attendanceState,
              onAddStaff = viewModel::addStaffMember,
              onRecordAdvance = viewModel::recordStaffAdvance,
              onDeleteStaff = viewModel::deleteStaffMember,
              onCheckIn = viewModel::checkInStaff,
              onCheckOut = viewModel::checkOutAttendance,
            )
          }
        }
      }
    }
  }
}

package com.example.ui.screens

import com.example.data.model.CalendarEventItem
import com.example.utils.LocalAppStrings
import com.example.ui.theme.BackgroundDeep
import com.example.ui.theme.BackgroundSurface
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHigh
import com.example.ui.theme.SurfaceModal
import com.example.ui.theme.SurfaceInput
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.hermesPageBackground
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.dock.dockBottomGradientFade
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    eventsList: List<CalendarEventItem> = emptyList(),
    targetYear: Int? = null,
    targetMonthIndex: Int? = null,
    targetDay: Int? = null,
    onAddEvent: (CalendarEventItem) -> Unit = {},
    onUpdateEvent: (CalendarEventItem) -> Unit = {},
    onDeleteEvent: (String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Current year / month / day state automatically detected from system clock
    val todayCal = remember { Calendar.getInstance() }
    val realTodayYear = remember { todayCal.get(Calendar.YEAR) }
    val realTodayMonth = remember { todayCal.get(Calendar.MONTH) }
    val realTodayDay = remember { todayCal.get(Calendar.DAY_OF_MONTH) }

    var currentYear by remember { mutableIntStateOf(targetYear ?: realTodayYear) }
    var currentMonthIndex by remember { mutableIntStateOf(targetMonthIndex ?: realTodayMonth) }
    var selectedDayNumber by remember { mutableIntStateOf(targetDay ?: realTodayDay) }

    LaunchedEffect(targetYear, targetMonthIndex, targetDay) {
        if (targetYear != null) currentYear = targetYear
        if (targetMonthIndex != null) currentMonthIndex = targetMonthIndex
        if (targetDay != null) selectedDayNumber = targetDay
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Calendar, 1 = Agendă
    var selectedViewMode by remember { mutableStateOf("Month") } // "Month", "Week", "Day"
    var isViewModeDropdownOpen by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var isAddEventDialogOpen by remember { mutableStateOf(false) }
    var defaultHourForNewEvent by remember { mutableIntStateOf(10) }

    val todayDateString = remember(realTodayYear, realTodayMonth, realTodayDay) {
        String.format(Locale.ROOT, "%04d-%02d-%02d", realTodayYear, realTodayMonth + 1, realTodayDay)
    }

    // Events added in this screen before the app hands the saved list back. It starts empty: the
    // original showed made-up meetings here, which would only confuse someone using a real calendar.
    var localEventsList by remember { mutableStateOf(emptyList<CalendarEventItem>()) }

    val strings = LocalAppStrings.current
    val activeEventsList = if (eventsList.isNotEmpty()) eventsList else localEventsList

    val fullMonthNames = arrayOf(
        strings.january, strings.february, strings.march, strings.april,
        strings.may, strings.june, strings.july, strings.august,
        strings.september, strings.october, strings.november, strings.december
    )
    val monthNames = fullMonthNames.map { if (it.length > 3) it.take(3) else it }.toTypedArray()
    val weekDays = listOf(
        strings.sun, strings.mon, strings.tue, strings.wed,
        strings.thu, strings.fri, strings.sat
    )

    // Dynamic calculations for current month grid
    val currentMonthCalendar = remember(currentYear, currentMonthIndex) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val daysInMonth = currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = currentMonthCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed offset

    // Filter events for selected day string (YYYY-MM-DD)
    val selectedDateString = String.format(Locale.ROOT, "%04d-%02d-%02d", currentYear, currentMonthIndex + 1, selectedDayNumber)
    val eventsForSelectedDay = remember(selectedDateString, activeEventsList, searchQuery) {
        activeEventsList.filter { ev ->
            if (searchQuery.isNotBlank()) {
                ev.title.contains(searchQuery, ignoreCase = true) || ev.location.contains(searchQuery, ignoreCase = true) || ev.category.contains(searchQuery, ignoreCase = true)
            } else {
                ev.dateString == selectedDateString
            }
        }
    }

    var horizontalDragAccumulator by remember { mutableFloatStateOf(0f) }

    BackHandler {
        when {
            isAddEventDialogOpen -> isAddEventDialogOpen = false
            isViewModeDropdownOpen -> isViewModeDropdownOpen = false
            isSearching -> {
                isSearching = false
                searchQuery = ""
            }
            activeTab != 0 -> activeTab = 0
            else -> onBack()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDragAccumulator = 0f },
                    onDragEnd = {
                        // Swipe left-to-right (drag right): open navigation drawer or navigate back
                        if (horizontalDragAccumulator > 70f) {
                            onOpenDrawer()
                        }
                        horizontalDragAccumulator = 0f
                    },
                    onDragCancel = { horizontalDragAccumulator = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDragAccumulator += dragAmount
                    }
                )
            }
            .hermesPageBackground()
            .testTag("calendar_screen_container"),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 4.dp)
            ) {
            // -----------------------------------------------------------------
            // TOP BAR: Real-time Weather Widget + Month Navigator
            // -----------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Real-time Dynamic Weather Widget
                RealTimeWeatherPill()

                // Month Navigator (< Aug 2026 >)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentMonthIndex == 0) {
                                currentMonthIndex = 11
                                currentYear--
                            } else {
                                currentMonthIndex--
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Luna anterioară",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "${monthNames[currentMonthIndex]} $currentYear",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            if (currentMonthIndex == 11) {
                                currentMonthIndex = 0
                                currentYear++
                            } else {
                                currentMonthIndex++
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Luna următoare",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // -----------------------------------------------------------------
            // CONTROLS BAR: [Calendar | Agendă] Pill + View Mode Dropdown + Search
            // -----------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Segmented Toggle Pill (Calendar / Agendă)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (activeTab == 0) SurfaceCardHigh else Color.Transparent)
                                .clickable { activeTab = 0 }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.calendar,
                                fontSize = 13.sp,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 0) TextPrimary else TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (activeTab == 1) SurfaceCardHigh else Color.Transparent)
                                .clickable { activeTab = 1 }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.agenda,
                                fontSize = 13.sp,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 1) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // View mode dropdown pill (Month / Week / Day)
                    if (activeTab == 0) {
                        Box {
                            Surface(
                                onClick = { isViewModeDropdownOpen = true },
                                shape = RoundedCornerShape(20.dp),
                                color = SurfaceCard,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedViewMode,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isViewModeDropdownOpen,
                                onDismissRequest = { isViewModeDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.month) },
                                    onClick = {
                                        selectedViewMode = "Month"
                                        isViewModeDropdownOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.week) },
                                    onClick = {
                                        selectedViewMode = "Week"
                                        isViewModeDropdownOpen = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.day) },
                                    onClick = {
                                        selectedViewMode = "Day"
                                        isViewModeDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Search round icon button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .border(1.dp, BorderSubtle, CircleShape)
                            .clickable { isSearching = !isSearching },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = strings.search,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar expand if active
            AnimatedVisibility(visible = isSearching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchPlaceholder, color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -----------------------------------------------------------------
            // MAIN VIEW AREA: MONTH / WEEK / DAY / AGENDA (With bottom fade under dock)
            // -----------------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .dockBottomGradientFade(fadeHeight = 185.dp)
            ) {
                if (activeTab == 0) {
                    when (selectedViewMode) {
                        "Month" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                MonthViewGrid(
                                    currentYear = currentYear,
                                    currentMonthIndex = currentMonthIndex,
                                    daysInMonth = daysInMonth,
                                    firstDayOfWeek = firstDayOfWeek,
                                    selectedDayNumber = selectedDayNumber,
                                    realTodayYear = realTodayYear,
                                    realTodayMonth = realTodayMonth,
                                    realTodayDay = realTodayDay,
                                    weekDays = weekDays,
                                    eventsList = activeEventsList,
                                    onSelectDay = { day -> selectedDayNumber = day }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                UpcomingEventsSection(
                                    eventsForSelectedDay = eventsForSelectedDay,
                                    selectedDayNumber = selectedDayNumber,
                                    monthName = monthNames[currentMonthIndex],
                                    onDeleteEvent = { evId ->
                                        onDeleteEvent(evId)
                                        localEventsList = localEventsList.filter { it.id != evId }
                                    }
                                )

                                Spacer(modifier = Modifier.height(120.dp))
                            }
                        }

                        "Week" -> {
                            WeekViewSchedule(
                                currentYear = currentYear,
                                currentMonthIndex = currentMonthIndex,
                                selectedDayNumber = selectedDayNumber,
                                monthName = monthNames[currentMonthIndex],
                                eventsList = activeEventsList,
                                onSelectDay = { day -> selectedDayNumber = day },
                                onAddEventAtHour = { day, hour ->
                                    selectedDayNumber = day
                                    defaultHourForNewEvent = hour
                                    isAddEventDialogOpen = true
                                },
                                onDeleteEvent = { evId ->
                                    onDeleteEvent(evId)
                                    localEventsList = localEventsList.filter { it.id != evId }
                                }
                            )
                        }

                        "Day" -> {
                            DayViewTimeline(
                                currentYear = currentYear,
                                currentMonthIndex = currentMonthIndex,
                                selectedDayNumber = selectedDayNumber,
                                monthName = monthNames[currentMonthIndex],
                                eventsForDay = eventsForSelectedDay,
                                onAddEventAtHour = { hour ->
                                    defaultHourForNewEvent = hour
                                    isAddEventDialogOpen = true
                                },
                                onDeleteEvent = { evId ->
                                    onDeleteEvent(evId)
                                    localEventsList = localEventsList.filter { it.id != evId }
                                }
                            )
                        }
                    }
                } else {
                    // AGENDĂ TAB: Continuous list of events grouped by date
                    AgendaViewSection(
                        eventsList = activeEventsList,
                        searchQuery = searchQuery,
                        onDeleteEvent = { evId ->
                            onDeleteEvent(evId)
                            localEventsList = localEventsList.filter { it.id != evId }
                        }
                    )
                }
            }
        }

        // Green Floating Action Button at Bottom Right (sleek, compact shape in exact position above dock)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = EmeraldPrimary,
            contentColor = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 102.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable {
                    defaultHourForNewEvent = 10
                    isAddEventDialogOpen = true
                }
                .testTag("new_event_fab_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.addEvent,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.addEvent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

    // -----------------------------------------------------------------
    // PRO INTUITIVE EVENT CREATION DIALOG
    // -----------------------------------------------------------------
    if (isAddEventDialogOpen) {
        CreateEventDialog(
            selectedDateString = selectedDateString,
            selectedDayNumber = selectedDayNumber,
            monthName = monthNames[currentMonthIndex],
            initialHour = defaultHourForNewEvent,
            onDismiss = { isAddEventDialogOpen = false },
            onSaveEvent = { newEvent ->
                onAddEvent(newEvent)
                localEventsList = localEventsList + newEvent
                isAddEventDialogOpen = false
            }
        )
    }
}

// -----------------------------------------------------------------
// DYNAMIC REAL-TIME WEATHER & LOCATION WIDGET
// -----------------------------------------------------------------
data class WeatherCityItem(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double
)

val presetWeatherCities = listOf(
    WeatherCityItem("București", "România", 44.4323, 26.1063),
    WeatherCityItem("Timișoara", "România", 45.7537, 21.2257),
    WeatherCityItem("Cluj-Napoca", "România", 46.7712, 23.6236),
    WeatherCityItem("Iași", "România", 47.1585, 27.6014),
    WeatherCityItem("Constanța", "România", 44.1807, 28.6343),
    WeatherCityItem("Brașov", "România", 45.6580, 25.6012),
    WeatherCityItem("Craiova", "România", 44.3302, 23.7949),
    WeatherCityItem("Sibiu", "România", 45.7983, 24.1256),
    WeatherCityItem("Oradea", "România", 47.0515, 21.9402),
    WeatherCityItem("Galați", "România", 45.4353, 28.0080),
    WeatherCityItem("Arad", "România", 46.1866, 21.3123),
    WeatherCityItem("Pitești", "România", 44.8565, 24.8692),
    WeatherCityItem("Ploiești", "România", 44.9365, 26.0129),
    WeatherCityItem("Suceava", "România", 47.6514, 26.2552),
    WeatherCityItem("Târgu Mureș", "România", 46.5456, 24.5625),
    WeatherCityItem("Baia Mare", "România", 47.6567, 23.5850),
    WeatherCityItem("Alba Iulia", "România", 46.0677, 23.5712),
    WeatherCityItem("Bacău", "România", 46.5670, 26.9146),
    WeatherCityItem("Botoșani", "România", 47.7420, 26.6661),
    WeatherCityItem("Deva", "România", 45.8778, 22.9031),
    WeatherCityItem("Reșița", "România", 45.3008, 21.8892),
    WeatherCityItem("Piatra Neamț", "România", 46.9275, 26.3708),
    WeatherCityItem("Târgoviște", "România", 44.9254, 25.4566),
    WeatherCityItem("Focșani", "România", 45.6960, 27.1851),
    WeatherCityItem("Bistrița", "România", 47.1333, 24.5000),
    WeatherCityItem("Tulcea", "România", 45.1783, 28.8062),
    WeatherCityItem("Slatina", "România", 44.4300, 24.3633),
    WeatherCityItem("Vaslui", "România", 46.6407, 27.7276),
    WeatherCityItem("Drobeta-Turnu Severin", "România", 44.6369, 22.6597),
    WeatherCityItem("Miercurea Ciuc", "România", 46.3606, 25.8016),
    WeatherCityItem("Sfântu Gheorghe", "România", 45.8622, 25.7881),
    WeatherCityItem("Zalău", "România", 47.1914, 23.0569),
    WeatherCityItem("Chișinău", "Moldova", 47.0105, 28.8638),
    WeatherCityItem("London", "Marea Britanie", 51.5074, -0.1278),
    WeatherCityItem("Paris", "Franța", 48.8566, 2.3522),
    WeatherCityItem("Roma", "Italia", 41.9028, 12.4964),
    WeatherCityItem("Berlin", "Germania", 52.5200, 13.4050),
    WeatherCityItem("Madrid", "Spania", 40.4168, -3.7038),
    WeatherCityItem("New York", "SUA", 40.7128, -74.0060)
)

@Composable
fun RealTimeWeatherPill() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("weather_settings_prefs", Context.MODE_PRIVATE) }

    var isAutoGps by remember { mutableStateOf(prefs.getBoolean("is_auto_gps", false)) }
    var locationName by remember { mutableStateOf(prefs.getString("location_name", "Timișoara") ?: "Timișoara") }
    var cityLat by remember { mutableDoubleStateOf(prefs.getFloat("city_lat", 45.7537f).toDouble()) }
    var cityLon by remember { mutableDoubleStateOf(prefs.getFloat("city_lon", 21.2257f).toDouble()) }

    var liveTemperature by remember { mutableIntStateOf(22) }
    var weatherCondition by remember { mutableStateOf("Cer Senin") }
    var windSpeed by remember { mutableIntStateOf(10) }
    var currentHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var showLocationDialog by remember { mutableStateOf(false) }

    // Periodic weather & location fetcher coroutine
    LaunchedEffect(locationName, isAutoGps, cityLat, cityLon) {
        while (true) {
            val cal = Calendar.getInstance()
            currentHour = cal.get(Calendar.HOUR_OF_DAY)

            if (isAutoGps) {
                try {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPerm) {
                        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                        val lastLoc = locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            ?: locManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                        if (lastLoc != null) {
                            cityLat = lastLoc.latitude
                            cityLon = lastLoc.longitude
                            val geocoder = Geocoder(context, Locale.getDefault())
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val city = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "Locația Mea"
                                if (city.isNotBlank()) {
                                    locationName = city
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Attempt Open-Meteo live API query for selected lat/lon
            try {
                withContext(Dispatchers.IO) {
                    val latStr = String.format(Locale.US, "%.4f", cityLat)
                    val lonStr = String.format(Locale.US, "%.4f", cityLon)
                    val apiUrl = "https://api.open-meteo.com/v1/forecast?latitude=$latStr&longitude=$lonStr&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&current_weather=true"
                    // Offline airplane mode keeps the app phone-local: no weather request leaves the phone.
                    if (com.mobilefork.hermesagent.data.HermesNetworkPolicy.isExternalNetworkBlocked(context, apiUrl)) return@withContext
                    val conn = java.net.URL(apiUrl).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; App)")

                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)

                        var temp: Int? = null
                        var wSpeed: Int? = null
                        var wCode: Int? = null

                        if (json.has("current_weather")) {
                            val cw = json.getJSONObject("current_weather")
                            temp = cw.getDouble("temperature").toInt()
                            wSpeed = cw.getDouble("windspeed").toInt()
                            wCode = cw.getInt("weathercode")
                        } else if (json.has("current")) {
                            val c = json.getJSONObject("current")
                            temp = c.getDouble("temperature_2m").toInt()
                            wSpeed = c.getDouble("wind_speed_10m").toInt()
                            wCode = c.getInt("weather_code")
                        }

                        if (temp != null && wSpeed != null && wCode != null) {
                            liveTemperature = temp
                            windSpeed = wSpeed
                            weatherCondition = when (wCode) {
                                0 -> "Cer Senin"
                                1 -> "Preponderent Senin"
                                2 -> "Parțial Înnorat"
                                3 -> "Înnorat"
                                45, 48 -> "Ceață"
                                51, 53, 55 -> "Burniță"
                                56, 57 -> "Burniță Înghețată"
                                61, 63, 65 -> "Ploaie"
                                66, 67 -> "Ploaie Înghețată"
                                71, 73, 75 -> "Ninsoare"
                                77 -> "Grindină"
                                80, 81, 82 -> "Averse de Ploaie"
                                85, 86 -> "Averse de Ninsoare"
                                95 -> "Furtună"
                                96, 99 -> "Furtună cu Grindină"
                                else -> if (temp > 24) "Însorit" else "Înnorat"
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            delay(30000) // refresh every 30s
        }
    }

    // Top-left weather layout directly on canvas (NO enclosing circle or pill border)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { showLocationDialog = true }
            .padding(vertical = 2.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Direct weather icon (no circle or background container around it)
        Icon(
            imageVector = when {
                weatherCondition.contains("Ploaie", ignoreCase = true) || weatherCondition.contains("Averse", ignoreCase = true) -> Icons.Default.Grain
                weatherCondition.contains("Ninsoare", ignoreCase = true) -> Icons.Default.AcUnit
                currentHour in 20..23 || currentHour in 0..5 -> Icons.Default.NightsStay
                weatherCondition.contains("Însorit", ignoreCase = true) || liveTemperature > 27 -> Icons.Default.WbSunny
                else -> Icons.Default.WbCloudy
            },
            contentDescription = weatherCondition,
            tint = when {
                weatherCondition.contains("Ploaie", ignoreCase = true) || weatherCondition.contains("Averse", ignoreCase = true) -> Color(0xFF64B5F6)
                weatherCondition.contains("Ninsoare", ignoreCase = true) -> Color(0xFFE0F7FA)
                currentHour in 20..23 || currentHour in 0..5 -> Color(0xFF90CAF9)
                weatherCondition.contains("Însorit", ignoreCase = true) || liveTemperature > 27 -> Color(0xFFFFC107)
                else -> Color(0xFFB0BEC5)
            },
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(verticalArrangement = Arrangement.Center) {
            // Line 1: Temperature & Condition
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${liveTemperature}°C",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = weatherCondition,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Line 2: Location Name + GPS Icon + Dropdown Arrow + Wind Speed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isAutoGps) Icons.Default.MyLocation else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = locationName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Schimbă locația",
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "• 💨 $windSpeed km/h",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isAutoGps = true
            prefs.edit().putBoolean("is_auto_gps", true).apply()
        }
    }

    if (showLocationDialog) {
        LocationSelectionDialog(
            currentLocationName = locationName,
            isAutoGps = isAutoGps,
            onDismiss = { showLocationDialog = false },
            onSelectAutoGps = {
                showLocationDialog = false
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPerm) {
                    isAutoGps = true
                    prefs.edit().putBoolean("is_auto_gps", true).apply()
                } else {
                    locationPermLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            onSelectCity = { selectedCity ->
                isAutoGps = false
                locationName = selectedCity.name
                cityLat = selectedCity.lat
                cityLon = selectedCity.lon
                prefs.edit()
                    .putBoolean("is_auto_gps", false)
                    .putString("location_name", selectedCity.name)
                    .putFloat("city_lat", selectedCity.lat.toFloat())
                    .putFloat("city_lon", selectedCity.lon.toFloat())
                    .apply()
                showLocationDialog = false
            }
        )
    }
}

@Composable
private fun LocationSelectionDialog(
    currentLocationName: String,
    isAutoGps: Boolean,
    onDismiss: () -> Unit,
    onSelectAutoGps: () -> Unit,
    onSelectCity: (WeatherCityItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<WeatherCityItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Live Geocoding search (Open-Meteo Geocoding API + Android Geocoder fallback)
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            isSearching = true
            withContext(Dispatchers.IO) {
                val list = mutableListOf<WeatherCityItem>()
                try {
                    val encoded = java.net.URLEncoder.encode(q, "UTF-8")
                    val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=15&language=ro&format=json"
                    // Same rule for the city search.
                    if (com.mobilefork.hermesagent.data.HermesNetworkPolicy.isExternalNetworkBlocked(context, url)) throw java.io.IOException("offline")
                    val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 4500
                    conn.readTimeout = 4500
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; App)")
                    if (conn.responseCode == 200) {
                        val resp = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(resp)
                        if (json.has("results")) {
                            val arr = json.getJSONArray("results")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val name = obj.getString("name")
                                val country = if (obj.has("country")) obj.getString("country") else "România"
                                val admin1 = if (obj.has("admin1")) obj.getString("admin1") else ""
                                val lat = obj.getDouble("latitude")
                                val lon = obj.getDouble("longitude")

                                val displayRegion = if (admin1.isNotBlank() && !admin1.equals(name, ignoreCase = true)) "$admin1, $country" else country
                                list.add(WeatherCityItem(name, displayRegion, lat, lon))
                            }
                        }
                    }
                } catch (_: Exception) {}

                if (list.isEmpty()) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(q, 6)
                        if (!addresses.isNullOrEmpty()) {
                            for (addr in addresses) {
                                val name = addr.locality ?: addr.subAdminArea ?: addr.featureName ?: q
                                val country = addr.countryName ?: "România"
                                val admin = addr.adminArea ?: ""
                                val region = if (admin.isNotBlank()) "$admin, $country" else country
                                list.add(WeatherCityItem(name, region, addr.latitude, addr.longitude))
                            }
                        }
                    } catch (_: Exception) {}
                }

                searchResults = list.distinctBy { "${it.name.lowercase()}_${it.country.lowercase()}" }
                isSearching = false
            }
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onSelectAutoGps()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        val strings = LocalAppStrings.current
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceModal,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.liveWeather,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.selectCityOrVillage,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GPS Auto Location Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAutoGps) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else SurfaceCard,
                    border = BorderStroke(
                        1.dp,
                        if (isAutoGps) MaterialTheme.colorScheme.primary else BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPerm) {
                                onSelectAutoGps()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isAutoGps) MaterialTheme.colorScheme.primary else SurfaceCardHigh,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = if (isAutoGps) Color.White else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.useGps,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                if (isAutoGps) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = strings.liveWeather,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        if (isAutoGps) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchCityVillagePlaceholder, fontSize = 13.sp, color = TextMuted) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceInput,
                        unfocusedContainerColor = SurfaceInput,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (searchQuery.isBlank()) strings.weatherDetails.uppercase() else strings.search.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // List of Cities / Dynamic Search Results
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSearching) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.searchingWeb, fontSize = 12.sp, color = TextSecondary)
                        }
                    } else if (searchQuery.isNotBlank()) {
                        if (searchResults.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceCard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectCity(WeatherCityItem(searchQuery.trim(), "", 45.7537, 21.2257))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("${strings.changeLocation}: ${searchQuery.trim()}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            searchResults.forEach { city ->
                                val isSelected = (!isAutoGps && currentLocationName.equals(city.name, ignoreCase = true))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) SurfaceCardHigh else SurfaceCard,
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else BorderSubtle
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCity(city) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = city.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = city.country,
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Show Preset Popular Cities when search query is empty
                        presetWeatherCities.forEach { city ->
                            val isSelected = (!isAutoGps && currentLocationName.equals(city.name, ignoreCase = true))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SurfaceCardHigh else SurfaceCard,
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else BorderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCity(city) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = city.name,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${city.country})",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// MONTH VIEW GRID
// -----------------------------------------------------------------
@Composable
fun MonthViewGrid(
    currentYear: Int,
    currentMonthIndex: Int,
    daysInMonth: Int,
    firstDayOfWeek: Int,
    selectedDayNumber: Int,
    realTodayYear: Int = 2026,
    realTodayMonth: Int = Calendar.AUGUST,
    realTodayDay: Int = 2,
    weekDays: List<String>,
    eventsList: List<CalendarEventItem>,
    onSelectDay: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            // Weekdays Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCardHigh)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDays.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderSubtle)
            )

            val totalCells = firstDayOfWeek + daysInMonth
            val numRows = (totalCells + 6) / 7

            Column {
                for (rowIndex in 0 until numRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (colIndex in 0..6) {
                            val cellIndex = rowIndex * 7 + colIndex
                            val dayNumber = cellIndex - firstDayOfWeek + 1

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(0.5.dp, BorderSubtle)
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val isSelected = (dayNumber == selectedDayNumber)
                                    val isToday = (dayNumber == realTodayDay && currentMonthIndex == realTodayMonth && currentYear == realTodayYear)
                                    val dateStr = String.format(Locale.ROOT, "%04d-%02d-%02d", currentYear, currentMonthIndex + 1, dayNumber)
                                    val holidayEvent = eventsList.find { it.dateString == dateStr && it.isHoliday }
                                    val hasRegularEvent = eventsList.any { it.dateString == dateStr && !it.isHoliday }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (holidayEvent != null) Color(0xFFEF4444).copy(alpha = 0.12f) else Color.Transparent)
                                            .clickable { onSelectDay(dayNumber) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Surface(
                                                shape = CircleShape,
                                                color = EmeraldPrimary,
                                                border = if (isToday) BorderStroke(2.dp, TextPrimary) else null,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "$dayNumber",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        } else if (isToday) {
                                            Surface(
                                                shape = CircleShape,
                                                color = EmeraldPrimary.copy(alpha = 0.2f),
                                                border = BorderStroke(1.5.dp, EmeraldPrimary),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "$dayNumber",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldPrimary
                                                    )
                                                }
                                            }
                                        } else if (holidayEvent != null) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                Text(
                                                    text = "$dayNumber",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFEF4444)
                                                )
                                                Text(
                                                    text = "Adormirea M...",
                                                    fontSize = 8.sp,
                                                    color = Color(0xFFEF4444),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNumber",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary
                                                )
                                                if (hasRegularEvent) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF10A37F))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// WEEK VIEW SCHEDULE (Fully Functional)
// -----------------------------------------------------------------
@Composable
fun WeekViewSchedule(
    currentYear: Int,
    currentMonthIndex: Int,
    selectedDayNumber: Int,
    monthName: String,
    eventsList: List<CalendarEventItem>,
    onSelectDay: (Int) -> Unit,
    onAddEventAtHour: (Int, Int) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    // Determine start of week containing selectedDayNumber
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentYear)
        set(Calendar.MONTH, currentMonthIndex)
        set(Calendar.DAY_OF_MONTH, selectedDayNumber)
    }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 (Sun) to 7 (Sat)
    cal.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1)) // move to Sunday of that week

    val weekDays = remember(selectedDayNumber, currentMonthIndex, currentYear) {
        val list = mutableListOf<Pair<Int, String>>()
        val dayLabels = arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
        for (i in 0..6) {
            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
            list.add(Pair(dayNum, dayLabels[i]))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
    ) {
        // Week Days Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekDays.forEach { (dayNum, label) ->
                val isSelected = (dayNum == selectedDayNumber)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                        .clickable { onSelectDay(dayNum) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(text = label, fontSize = 10.sp, color = if (isSelected) Color.White else TextSecondary)
                    Text(
                        text = "$dayNum",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hourly timeline for selected week day
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            val dateStr = String.format(Locale.ROOT, "%04d-%02d-%02d", currentYear, currentMonthIndex + 1, selectedDayNumber)
            val dayEvents = eventsList.filter { it.dateString == dateStr }

            Text(
                text = "PROGRAM $selectedDayNumber $monthName",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            for (hour in 8..21) {
                val hourLabel = String.format(Locale.ROOT, "%02d:00", hour)
                val hourEvents = dayEvents.filter { it.startHour == hour }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = hourLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.width(50.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .background(SurfaceCardHigh)
                            .padding(8.dp)
                    ) {
                        if (hourEvents.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAddEventAtHour(selectedDayNumber, hour) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+ Adaugă eveniment la $hourLabel",
                                    fontSize = 12.sp,
                                    color = Color.Gray.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            hourEvents.forEach { ev ->
                                EventCardItem(event = ev, onDelete = { onDeleteEvent(ev.id) })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// -----------------------------------------------------------------
// DAY VIEW TIMELINE (Fully Functional)
// -----------------------------------------------------------------
@Composable
fun DayViewTimeline(
    currentYear: Int,
    currentMonthIndex: Int,
    selectedDayNumber: Int,
    monthName: String,
    eventsForDay: List<CalendarEventItem>,
    onAddEventAtHour: (Int) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceCard)
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$selectedDayNumber $monthName $currentYear",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${eventsForDay.size} evenimente programate",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        for (hour in 7..22) {
            val hourLabel = String.format(Locale.ROOT, "%02d:00", hour)
            val matchedEvents = eventsForDay.filter { it.startHour == hour }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = hourLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.width(55.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (matchedEvents.isNotEmpty()) SurfaceCardHigh else SurfaceCardHigh.copy(alpha = 0.4f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            if (matchedEvents.isEmpty()) onAddEventAtHour(hour)
                        }
                        .padding(10.dp)
                ) {
                    if (matchedEvents.isEmpty()) {
                        Text(
                            text = "Liber • Atinge pentru a adăuga",
                            fontSize = 11.sp,
                            color = Color.Gray.copy(alpha = 0.4f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            matchedEvents.forEach { ev ->
                                EventCardItem(event = ev, onDelete = { onDeleteEvent(ev.id) })
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

// -----------------------------------------------------------------
// UPCOMING EVENTS SECTION
// -----------------------------------------------------------------
@Composable
fun UpcomingEventsSection(
    eventsForSelectedDay: List<CalendarEventItem>,
    selectedDayNumber: Int,
    monthName: String,
    onDeleteEvent: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UPCOMING ($selectedDayNumber $monthName)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (eventsForSelectedDay.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "No events scheduled",
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                eventsForSelectedDay.forEachIndexed { index, item ->
                    EventCardItem(event = item, onDelete = { onDeleteEvent(item.id) })
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// AGENDA VIEW SECTION
// -----------------------------------------------------------------
@Composable
fun AgendaViewSection(
    eventsList: List<CalendarEventItem>,
    searchQuery: String,
    onDeleteEvent: (String) -> Unit
) {
    val filteredEvents = remember(eventsList, searchQuery) {
        if (searchQuery.isBlank()) eventsList else eventsList.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.location.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AGENDĂ EVENIMENTE (${filteredEvents.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(filteredEvents, key = { index, item -> "${item.id}_$index" }) { index, item ->
                EventCardItem(event = item, onDelete = { onDeleteEvent(item.id) }, showDate = true)
            }
        }
    }
}

// -----------------------------------------------------------------
// REUSABLE EVENT CARD COMPONENT
// -----------------------------------------------------------------
@Composable
fun EventCardItem(
    event: CalendarEventItem,
    onDelete: () -> Unit,
    showDate: Boolean = false
) {
    val categoryColor = when (event.category) {
        "Ședință" -> Color(0xFF10A37F)
        "Work" -> Color(0xFF2563EB)
        "Urgent" -> Color(0xFFEF4444)
        "Personal" -> Color(0xFF8B5CF6)
        "Sărbătoare" -> Color(0xFFF59E0B)
        else -> Color(0xFF10A37F)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isHoliday) Color(0xFFEF4444).copy(alpha = 0.12f) else SurfaceCardHigh
        ),
        border = BorderStroke(
            1.dp,
            if (event.isHoliday) Color(0xFFEF4444).copy(alpha = 0.35f) else BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (event.isHoliday) Icons.Default.Event else Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = categoryColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = event.category,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showDate) {
                            Text(
                                text = "${event.dateString} • ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Text(
                            text = event.time,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        if (event.location.isNotBlank()) {
                            Text(
                                text = " • ${event.location}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    if (event.description.isNotBlank()) {
                        Text(
                            text = event.description,
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (!event.isHoliday) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Șterge",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// INTUITIVE CREATE EVENT DIALOG
// -----------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEventDialog(
    selectedDateString: String,
    selectedDayNumber: Int,
    monthName: String,
    initialHour: Int,
    onDismiss: () -> Unit,
    onSaveEvent: (CalendarEventItem) -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(strings.meeting) }
    var startHour by remember { mutableIntStateOf(initialHour) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val categories = listOf(strings.meeting, strings.work, strings.personal, strings.urgent, strings.holiday)
    val titlePresets = listOf(strings.meeting, strings.work, strings.personal, strings.sync, strings.review)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${strings.newEvent} ($selectedDayNumber $monthName)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quick title presets
                Text(strings.quickSuggestions, fontSize = 11.sp, color = TextMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    titlePresets.forEach { preset ->
                        Surface(
                            onClick = { title = preset },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceCardHigh,
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("${strings.eventTitle} *", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category selection chips
                Text("${strings.category}:", fontSize = 11.sp, color = TextMuted)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceCardHigh,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Hour selector row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.startHour, fontSize = 12.sp, color = TextMuted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (startHour > 0) startHour-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = TextPrimary)
                        }
                        Text(
                            text = String.format(Locale.ROOT, "%02d:00", startHour),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = { if (startHour < 23) startHour++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextPrimary)
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(strings.locationOrMeeting, color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.additionalNotes, color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val endHour = (startHour + 1).coerceAtMost(23)
                        val timeFormatted = String.format(Locale.ROOT, "%02d:00 - %02d:00", startHour, endHour)
                        val newEv = CalendarEventItem(
                            id = java.util.UUID.randomUUID().toString(),
                            title = title,
                            dateString = selectedDateString,
                            time = timeFormatted,
                            startHour = startHour,
                            category = category,
                            location = location.ifBlank { "Online" },
                            description = description
                        )
                        onSaveEvent(newEv)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(strings.save, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = TextSecondary)
            }
        }
    )
}

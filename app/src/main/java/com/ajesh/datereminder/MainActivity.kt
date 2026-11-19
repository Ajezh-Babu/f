package com.ajesh.datereminder

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    companion object { const val EXTRA_ADD = "open_add" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmReceiver.createChannel(this)
        val openAdd = intent?.getBooleanExtra(EXTRA_ADD, false) == true
        setContent { DarkTheme { ReminderApp(openAdd) } }
    }
}

private val Accent = Color(0xFF7C9CFF)
private val OnAccent = Color(0xFF0F1115)

@Composable
fun DarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, onPrimary = OnAccent,
            background = Color(0xFF0F1115), surface = Color(0xFF1A1D24),
            onBackground = Color(0xFFE6E8EE), onSurface = Color(0xFFE6E8EE),
            surfaceVariant = Color(0xFF242832), onSurfaceVariant = Color(0xFFB5BAC7)
        ), content = content
    )
}

@Composable
fun ReminderApp(openAddInitially: Boolean) {
    val ctx = LocalContext.current
    var reminders by remember { mutableStateOf(ReminderStore.load(ctx)) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var showAdd by remember { mutableStateOf(openAddInitially) }
    var exactOk by remember { mutableStateOf(AlarmScheduler.canScheduleExact(ctx)) }

    fun reload() { reminders = ReminderStore.load(ctx); exactOk = AlarmScheduler.canScheduleExact(ctx) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Re-check exact-alarm permission when returning from system Settings
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                val was = exactOk
                reload()
                if (!was && exactOk) AlarmScheduler.rescheduleAll(ctx)
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = Accent, contentColor = OnAccent) {
                Icon(Icons.Default.Add, contentDescription = "Add reminder")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp)) {
            Text("Reminders", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

            if (!exactOk) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2A1A)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                        if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                        )
                    }
                ) {
                    Text("Tap to allow exact alarms so reminders fire at the exact minute.",
                        Modifier.padding(12.dp), fontSize = 13.sp, color = Color(0xFFFFD9A8))
                }
            }

            CalendarView(month, selected, reminders,
                onPrev = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
                onSelect = { selected = it })

            Spacer(Modifier.height(12.dp))
            Text(selected.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))

            val day = reminders.filter { it.date == selected }
            if (day.isEmpty()) {
                Text("No reminders for this day", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 90.dp)) {
                items(day, key = { it.id }) { r ->
                    ReminderRow(r,
                        onToggle = { ReminderStore.setDone(ctx, r.id, it); reload() },
                        onDelete = { ReminderStore.delete(ctx, r.id); reload() })
                }
            }
        }
    }

    if (showAdd) {
        AddDialog(initialDate = selected,
            onDismiss = { showAdd = false },
            onSave = { title, millis ->
                ReminderStore.add(ctx, title, millis)
                selected = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                month = YearMonth.from(selected)
                showAdd = false
                reload()
            })
    }
}

@Composable
fun CalendarView(
    month: YearMonth, selected: LocalDate, reminders: List<Reminder>,
    onPrev: () -> Unit, onNext: () -> Unit, onSelect: (LocalDate) -> Unit
) {
    val marked = remember(reminders) { reminders.filter { !it.done }.map { it.date }.toSet() }
    val weekDays = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, "Previous month") }
                Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
                    Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "Next month") }
            }
            Row(Modifier.fillMaxWidth()) {
                weekDays.forEach {
                    Text(it.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                        Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            val offset = month.atDay(1).dayOfWeek.value % 7 // Sunday = 0
            val weeks = (offset + month.lengthOfMonth() + 6) / 7
            for (w in 0 until weeks) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0..6) {
                        val dayNum = w * 7 + c - offset + 1
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..month.lengthOfMonth()) {
                                val date = month.atDay(dayNum)
                                val isSel = date == selected
                                val isToday = date == LocalDate.now()
                                Column(
                                    Modifier.fillMaxSize().clip(CircleShape)
                                        .background(if (isSel) Accent else Color.Transparent)
                                        .clickable { onSelect(date) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("$dayNum", fontSize = 14.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) OnAccent else if (isToday) Accent
                                        else MaterialTheme.colorScheme.onSurface)
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(
                                        if (date in marked) (if (isSel) OnAccent else Accent) else Color.Transparent))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderRow(r: Reminder, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val time = Instant.ofEpochMilli(r.timeMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = r.done, onCheckedChange = onToggle)
            Column(Modifier.weight(1f)) {
                Text(r.title, fontSize = 16.sp,
                    textDecoration = if (r.done) TextDecoration.LineThrough else null,
                    color = if (r.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                Text(time, fontSize = 13.sp, color = Accent)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AddDialog(initialDate: LocalDate, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(LocalTime.now().plusMinutes(5).withSecond(0).withNano(0)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it; error = null }, label = { Text("What to remember?") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = {
                    DatePickerDialog(ctx, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                        date.year, date.monthValue - 1, date.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp))
                    Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")))
                }
                OutlinedButton(onClick = {
                    TimePickerDialog(ctx, { _, h, m -> time = LocalTime.of(h, m) },
                        time.hour, time.minute, false).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccessTime, null); Spacer(Modifier.width(8.dp))
                    Text(time.format(DateTimeFormatter.ofPattern("h:mm a")))
                }
                error?.let { Text(it, color = Color(0xFFFF8A80), fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val millis = LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                when {
                    title.isBlank() -> error = "Enter a title"
                    millis <= System.currentTimeMillis() -> error = "Pick a future date and time"
                    else -> onSave(title.trim(), millis)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

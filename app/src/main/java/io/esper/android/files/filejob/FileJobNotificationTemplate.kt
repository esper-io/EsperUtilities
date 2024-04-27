package io.esper.android.files.filejob

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.esper.android.files.R
import io.esper.android.files.util.NotificationChannelTemplate
import io.esper.android.files.util.NotificationTemplate

val fileJobNotificationTemplate =
    NotificationTemplate(
        NotificationChannelTemplate(
            "file_job",
            R.string.notification_channel_file_job_name,
            NotificationManagerCompat.IMPORTANCE_LOW,
            descriptionRes = R.string.notification_channel_file_job_description,
            showBadge = false
        ),
        colorRes = R.color.color_primary,
        smallIcon = R.drawable.notification_icon,
        ongoing = true,
        onlyAlertOnce = true,
        category = NotificationCompat.CATEGORY_PROGRESS,
        priority = NotificationCompat.PRIORITY_LOW
    )

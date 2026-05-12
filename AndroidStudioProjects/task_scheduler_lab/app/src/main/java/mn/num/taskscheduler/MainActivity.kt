package mn.num.taskscheduler

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// Аппын үндсэн Activity.
// Энэ Activity нь бүх Fragment-уудыг дотороо харуулдаг container үүрэгтэй.
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // activity_main.xml layout-ийг энэ Activity дээр байрлуулж байна
        setContentView(R.layout.activity_main)

        // Апп анх нээгдэх үед WelcomeFragment-г харуулна
        // savedInstanceState == null гэдэг нь activity анх удаа үүсэж байгаа гэсэн үг
        // (screen rotation гэх мэт үед дахин fragment нэмэгдэхээс сэргийлдэг)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()

                // fragment_container дотор WelcomeFragment-г байрлуулж байна
                .replace(R.id.fragment_container, WelcomeFragment())

                // transaction-г ажиллуулна
                .commit()
        }
    }
}
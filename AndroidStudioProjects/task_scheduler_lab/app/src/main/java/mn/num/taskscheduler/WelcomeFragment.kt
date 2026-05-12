package mn.num.taskscheduler

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

// WelcomeFragment нь апп нээгдэхэд эхлээд харагдах welcome дэлгэц юм
class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    // Fragment дэлгэц үүссэний дараа ажиллах функц
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "Get Started" товчийг layout-аас авч байна
        val btnGetStarted = view.findViewById<Button>(R.id.btnGetStarted)

        // Товч дарахад HomeFragment рүү шилжинэ
        btnGetStarted.setOnClickListener {

            parentFragmentManager.beginTransaction()

                // WelcomeFragment → HomeFragment солих
                .replace(R.id.fragment_container, HomeFragment())

                // Back товч дарахад буцах боломжтой болгоно
                .addToBackStack(null)

                // Fragment transaction-г ажиллуулна
                .commit()
        }
    }
}
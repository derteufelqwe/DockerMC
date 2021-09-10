import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import java.io.Serializable

class PrintAppender(layout: Layout<out Serializable>) : AbstractAppender("PrintAppender", null, layout) {

    override fun append(event: LogEvent?) {
        print(this.layout.toSerializable(event))
    }
}



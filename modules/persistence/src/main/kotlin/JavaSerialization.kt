
import org.kalasim.demo.MM1Queue
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


fun main() {
//    val sim = EmergencyRoom(SetupAvoidanceNurse)
    val sim = MM1Queue().apply { run(10) }

    // run for a week
//        run(24 * 14)

    val fileOutputStream = FileOutputStream("yourfile.dat")
    val objectOutputStream = ObjectOutputStream(fileOutputStream)
    objectOutputStream.writeObject(sim)
    objectOutputStream.flush()
    objectOutputStream.close()

    val fileInputStream = FileInputStream("yourfile.dat")
    val objectInputStream = ObjectInputStream(fileInputStream)
    val restoredSim: MM1Queue = objectInputStream.readObject() as MM1Queue
    objectInputStream.close()

    // analysis
    restoredSim.run(10)
//    sim.testSim()
}

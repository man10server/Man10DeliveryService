package ltotj.minecraft.man10deliveryservice

import com.google.common.math.IntMath.pow
import ltotj.minecraft.man10deliveryservice.Main.Companion.plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.io.BukkitObjectInputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.log10

object Utility {

    fun createClickEventText_run(text:String,command:String):Component{
        return text(text).clickEvent(ClickEvent.runCommand(command))
    }

    fun countAirPocket(inv: Inventory):Int{//Playerのinv専用 関数名にPlayerってつけるべきでしたね...
        var count=0
        for(i in 0 until 36){
            if(inv.contents?.get(i) ==null)count++
        }
        return count
    }

    fun getDateForMySQL(date: Date): String? {
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd HHH:mm:ss")
        return df.format(date)
    }

    fun getYenString(money:Double):String{//１２桁まで対応
        val yen=StringBuilder().append("円")
        val integerPart= floor(money)
        if(integerPart!=0.0) {
            val end = floor(log10(integerPart)).toInt() / 3
            for (i in 0 until end) {
                for (j in 0 until 3) {
                    yen.append(floor((integerPart - floor(integerPart / pow(10, i * 3 + j + 1)) * pow(10, i * 3 + j + 1)) / pow(10, i * 3 + j)).toInt())
                }
                yen.append(",")
            }
            yen.append(floor(integerPart / pow(10, end * 3)).toInt().toString().reversed())
            yen.reverse()
        }
        else yen.append("0").reverse()
        return yen.toString()
    }

//    fun itemFromBase64(data: String): ItemStack? = try {
//        val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
//        val dataInput = BukkitObjectInputStream(inputStream)
//        val items = arrayOfNulls<ItemStack>(dataInput.readInt())
//
//        // Read the serialized inventory
//        for (i in items.indices) {
//            items[i] = dataInput.readObject() as ItemStack
//        }
//
//        dataInput.close()
//        items[0]
//    } catch (e: Exception) {
//        null
//    }

    fun setNBTInt(item:ItemStack,namespacedKey: String,value:Int){
        val meta=item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(plugin,namespacedKey), PersistentDataType.INTEGER,value)
        item.itemMeta=meta
    }

    fun setNBTString(item:ItemStack,namespacedKey: String,value:String){
        val meta=item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(plugin,namespacedKey), PersistentDataType.STRING,value)
        item.itemMeta=meta
    }

    fun setNBTDouble(item:ItemStack,namespacedKey: String,value:Double){
        val meta=item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(plugin,namespacedKey), PersistentDataType.DOUBLE,value)
        item.itemMeta=meta
    }

    fun getNBTInt(item:ItemStack,namespacedKey:String):Int{
        val meta=item.itemMeta?:return -1
        return meta.persistentDataContainer[NamespacedKey(plugin,namespacedKey), PersistentDataType.INTEGER]?:-1
    }

    fun getNBTString(item:ItemStack,namespacedKey:String):String{
        val meta=item.itemMeta?:return ""
        return meta.persistentDataContainer[NamespacedKey(plugin,namespacedKey), PersistentDataType.STRING]?:""
    }

    fun getNBTDouble(item:ItemStack,namespacedKey: String):Double{
        val meta=item.itemMeta?:return 0.0
        return meta.persistentDataContainer[NamespacedKey(plugin,namespacedKey), PersistentDataType.DOUBLE]?:0.0
    }

    fun createGUIItem(material: Material, amount: Int, name: String, lore: List<String>):ItemStack{
        val item=ItemStack(material, amount)
        val meta=item.itemMeta
        meta.displayName(Component.text(name))
        meta.lore(listToComponent(lore))
        item.itemMeta = meta
        return item
    }

    fun createGUIItem(material: Material, amount: Int, name: String):ItemStack{
        val item=ItemStack(material, amount)
        val meta=item.itemMeta
        meta.displayName(Component.text(name))
        item.itemMeta = meta
        return item
    }

    private fun listToComponent(list: List<String>):List<Component>{
        val cList=ArrayList<Component>()
        for(i in list.indices){
            cList.add(Component.text(list[i]))
        }
        return cList
    }

}
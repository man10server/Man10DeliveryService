package ltotj.minecraft.man10deliveryservice.order

import ltotj.minecraft.man10deliveryservice.Main
import ltotj.minecraft.man10deliveryservice.Main.Companion.boxOpeningList
import ltotj.minecraft.man10deliveryservice.Main.Companion.executor
import ltotj.minecraft.man10deliveryservice.Main.Companion.pluginTitle
import ltotj.minecraft.man10deliveryservice.MySQLManager
import ltotj.minecraft.man10deliveryservice.Utility.countAirPocket
import ltotj.minecraft.man10deliveryservice.Utility.getDateForMySQL
import ltotj.minecraft.man10deliveryservice.Utility.getNBTInt
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*

object ItemBox: Listener {

    private val mysql= MySQLManager(Main.plugin, pluginTitle)

    @EventHandler
    fun containerOpenEvent(e: PlayerInteractEvent){
        val item=e.item?:return
        val order_id=getNBTInt(item,"order_id")
        if(order_id==-1)return
        e.isCancelled=true
        if(e.hand==null||(e.hand!=null&&e.hand!=EquipmentSlot.HAND))return
        if(!e.player.hasPermission("mdelivery.player")){
            e.player.sendMessage("§4権限なし")
            return
        }
        if(e.player.hasPermission("mdlivery.admin")&&getNBTInt(item,"admin_box")==1){
            executor.execute{
                val result= mysql.query("select amount,slot1,slot2,slot3,slot4,slot5,slot6,slot7,slot8 from delivery_order where order_id=$order_id;")
                if(result==null){
                    e.player.sendMessage("§4データーベース接続エラー")
                    mysql.close()
                    return@execute
                }
                result.next()
                if(result.row==0){
                    e.player.sendMessage("§4アイテムボックスのデータが見つかりませんでした")
                    result.close()
                    mysql.close()
                    return@execute
                }
                if(countAirPocket(e.player.inventory)<result.getInt("amount")){
                    e.player.sendMessage("§4インベントリに§d${countAirPocket(e.player.inventory)}個§4の空きを作ってください")
                    result.close()
                    mysql.close()
                    return@execute
                }
                else{
                    for(i in 1..8){
                        val boxedItem= ItemStack.deserializeBytes(result.getBytes("slot$i")?:continue)
                        e.player.inventory.addItem(boxedItem)
                    }
                    e.player.sendMessage("§aアドミンボックスを開封しました")
                    e.player.inventory.remove(item)
                }
                result.close()
                mysql.close()
                return@execute
            }
            return
        }
        if(!Main.available){
            e.player.sendMessage("§4[${Main.pluginTitle}]はただいま停止中です")
            return
        }
        else{
            if(boxOpeningList.contains(e.player.uniqueId))return
            boxOpeningList.add(e.player.uniqueId)
            executor.execute {
                val result=mysql.query("select sender_uuid,amount,slot1,slot2,slot3,slot4,slot5,slot6,slot7,slot8,box_status from delivery_order where order_id=$order_id;")
                if(result==null){
                    e.player.sendMessage("§4データベース接続エラー")
                    mysql.close()
                    return@execute
                }
                result.next()
                if(result.row==0){
                    e.player.sendMessage("§4アイテムボックスが見つかりませんでした")
                    result.close()
                    mysql.close()
                    boxOpeningList.remove(e.player.uniqueId)
                    return@execute
                }
                if(result.getBoolean("box_status")){
                    e.player.sendMessage("§4既に開封されたボックスです")
                    println("§4${e.player.name}が開封済みのボックスを所持していました：order_id-$order_id")
                    e.player.inventory.remove(item)
                    result.close()
                    mysql.close()
                    boxOpeningList.remove(e.player.uniqueId)
                    return@execute
                }
                if(countAirPocket(e.player.inventory)<result.getInt("amount")){
                    e.player.sendMessage("§4インベントリがいっぱいです §d${countAirPocket(e.player.inventory)}個§4の空きを作って再度開けてください")
                    result.close()
                    mysql.close()
                    boxOpeningList.remove(e.player.uniqueId)
                    return@execute
                }
                if(Bukkit.getPlayer(e.player.uniqueId)!=null) {
                    if (mysql.execute("update delivery_order set box_status=true,opener_name='${e.player.name}',opener_uuid='${e.player.uniqueId}',opened_date='${getDateForMySQL(Date())}' where order_id=$order_id;")) {
                        e.player.inventory.remove(item)
                        for (i in 1..8) {
                            val boxedItem = ItemStack.deserializeBytes(result.getBytes("slot$i") ?: continue)
                            e.player.inventory.addItem(boxedItem)
                        }
                        e.player.playSound(e.player.location, Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F)
                        e.player.sendMessage("§aボックスを開封しました")
                        boxOpeningList.remove(e.player.uniqueId)
                        Bukkit.getPlayer(UUID.fromString(result.getString("sender_uuid")))?.sendMessage("§e§l${e.player.name}があなたの送ったボックスを開封しました！")
                    } else {
                        e.player.sendMessage("§4ボックスを開封できませんでした")
                    }
                    result.close()
                    mysql.close()
                    boxOpeningList.remove(e.player.uniqueId)
                    return@execute
                }
            }
        }
    }
}
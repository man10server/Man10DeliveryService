package ltotj.minecraft.man10deliveryservice.order

import ltotj.minecraft.man10deliveryservice.Main
import ltotj.minecraft.man10deliveryservice.Main.Companion.con
import ltotj.minecraft.man10deliveryservice.Main.Companion.disableItems
import ltotj.minecraft.man10deliveryservice.Main.Companion.playerLastSendingData
import ltotj.minecraft.man10deliveryservice.Main.Companion.plugin
import ltotj.minecraft.man10deliveryservice.Main.Companion.pluginTitle
import ltotj.minecraft.man10deliveryservice.Main.Companion.sendButtonName
import ltotj.minecraft.man10deliveryservice.Main.Companion.vault
import ltotj.minecraft.man10deliveryservice.MySQLManager
import ltotj.minecraft.man10deliveryservice.Utility.countAirPocket
import ltotj.minecraft.man10deliveryservice.Utility.createClickEventText_run
import ltotj.minecraft.man10deliveryservice.Utility.createGUIItem
import ltotj.minecraft.man10deliveryservice.Utility.getDateForMySQL
import ltotj.minecraft.man10deliveryservice.Utility.getNBTDouble
import ltotj.minecraft.man10deliveryservice.Utility.getNBTString
import ltotj.minecraft.man10deliveryservice.Utility.getYenString
import ltotj.minecraft.man10deliveryservice.Utility.setNBTDouble
import ltotj.minecraft.man10deliveryservice.Utility.setNBTString
import ltotj.minecraft.man10deliveryservice.command.LogCommand
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.lang.Integer.min
import java.lang.Thread.sleep
import java.util.*

object DeliveryOrder: Listener {

    private val mysql= MySQLManager(plugin, pluginTitle)

    private fun returnItem(player:Player,contents:Array<ItemStack?>){
        var pocket=countAirPocket(player.inventory)
        for(i in 0 until 8){
            if(contents[i]!=null){
                if(pocket==0)player.world.dropItem(player.location,contents[i]!!)
                else {
                    player.inventory.addItem(contents[i]!!)
                    pocket--
                }
            }
        }
    }




    fun generateContainerSelectInv(receiver_name:String,boxName:String):Inventory{
        val wrappingList=con.getConfigurationSection("itemBox")!!.getKeys(false)
        val invSize=min(54,((wrappingList.size-1)/8)*9+9)
        val inv=Bukkit.createInventory(null,invSize, Component.text(Main.containerSelectGUIName))
        for(i in invSize-9 until invSize)inv.setItem(i, ItemStack(Material.WHITE_STAINED_GLASS_PANE))
        for(i in 0 until invSize/9)inv.setItem(i*9+8,ItemStack(Material.WHITE_STAINED_GLASS_PANE))
        for(i in 0 until min(wrappingList.size,invSize-(invSize/9 +1))){
            val item=createGUIItem(Material.valueOf(con.getString("itemBox.${wrappingList.elementAt(i)}.material")?:"CHEST"), 1, con.getString("itemBox.${wrappingList.elementAt(i)}.displayName")?:"ITEMBOX", listOf("§e追加料金：§4${getYenString(con.getDouble("itemBox.${wrappingList.elementAt(i)}.price"))}", "§eクリックで選択"))
            setNBTString(item, "wrapping", wrappingList.elementAt(i))
            val meta=item.itemMeta
            meta.setCustomModelData(con.getInt("itemBox.${wrappingList.elementAt(i)}.customModelData"))
            item.itemMeta=meta
            inv.setItem((i/8)*9+i%8,item)
        }
        val keyItem=ItemStack(Material.WHITE_STAINED_GLASS_PANE)
        setNBTString(keyItem,"boxName",boxName)
        setNBTString(keyItem,"receiver_name",receiver_name)
        inv.setItem(invSize -1,keyItem)
        return inv
    }

    private fun generateContainer(wrapping:String,keyItem:ItemStack):Inventory{
        val container=Bukkit.createInventory(null,9, Component.text(Main.containerGUIName))

        val receiver_name=getNBTString(keyItem, "receiver_name")
        val totalPrice=con.getDouble("itemBox.$wrapping.price")+con.getDouble("postage")
        val boxName=if (getNBTString(keyItem,"boxName")=="noName") con.getString("itemBox.$wrapping.displayName") else getNBTString(keyItem,"boxName")
        val sendButton= createGUIItem(Material.FEATHER,1, sendButtonName, listOf("§dクリックでアイテムを送信します","§aラッピング：${wrapping}"
                ,"§aコンテナ名：$boxName"
                ,"§e発送料金：§4${getYenString(totalPrice)}"))
        setNBTDouble(sendButton, "totalPrice", totalPrice)
        setNBTString(sendButton,"wrapping",wrapping)
        setNBTString(sendButton,"receiver_name",receiver_name)
        setNBTString(sendButton,"boxName",boxName!!)
        container.setItem(8,sendButton)

        return container
    }

    @EventHandler
    fun containerClickEvent(e:InventoryClickEvent){
        if(e.view.title()!= Component.text(Main.containerGUIName)||e.clickedInventory?.size!=9)return
        else if(e.slot==8){
            e.isCancelled=true
            if(!Main.available){
                e.whoClicked.sendMessage("§4[${Main.pluginTitle}]はただいま停止中です")
                return
            }
            val container=e.clickedInventory?.contents?.clone()?:return
            var count=0
            for(i in 0 until 8){
                if(container[i]!=null)count++
            }
            if(count==0){
                e.whoClicked.sendMessage("空のコンテナを送ることはできません")
                e.whoClicked.closeInventory()
                return
            }
            val keyItem=e.currentItem!!
            val totalPrice=getNBTDouble(keyItem,"totalPrice")
            if(vault.getBalance(e.whoClicked.uniqueId)<totalPrice){
                e.whoClicked.sendMessage("お金が足りません！")
                return
            }
            for(i in 0..7){
                if(container[i]!=null&&disableItems.contains(container[i]?.type.toString())){
                    e.whoClicked.sendMessage("§4${container[i]?.type}を送ることはできません")
                    return
                }
            }
            e.clickedInventory?.clear()
            e.whoClicked.closeInventory()
            Main.executor.execute {
                val receiver_name=getNBTString(keyItem,"receiver_name")
                val receiver_uuid=Bukkit.getOfflinePlayer(receiver_name).uniqueId.toString()
                val result= mysql.query("select owner_uuid,delivery_amount from player_status where owner_uuid='${receiver_uuid}';")
                if(result==null){
                    mysql.close()
                    Bukkit.getScheduler().runTask(plugin,Runnable {
                        returnItem(e.whoClicked as Player,container)
                    })
                    return@execute
                }
                if(!result.next()){
                    result.close()
                    mysql.close()
                    Bukkit.getScheduler().runTask(plugin,Runnable {
                        returnItem(e.whoClicked as Player,container)
                    })
                    return@execute
                }
                val sender_name=e.whoClicked.name
                val sender_uuid=e.whoClicked.uniqueId
                val date= Date()
                val wrapping=getNBTString(keyItem,"wrapping")
                val boxName= getNBTString(keyItem,"boxName")
                val query=StringBuilder()
                query.append("insert into delivery_order(sender_name,sender_uuid,receiver_name,receiver_uuid,order_date,wrapping,boxName,amount,slot1,slot2,slot3,slot4,slot5,slot6,slot7,slot8) " +
                        "values('$sender_name','$sender_uuid','$receiver_name','$receiver_uuid','${getDateForMySQL(date)}','$wrapping'" +
                        ",'$boxName','$count'")
                for(i in 0 until 8){
                    if(container?.get(i)==null){
                        query.append(",null")
                    }
                    else {
                        query.append(",'${container[i]?.serializeAsBytes()}'")
                    }
                }
                query.append(");")
                if(mysql.execute(query.toString())){
                    mysql.execute("update player_status set delivery_amount=${result.getInt("delivery_amount")+1} where owner_uuid='$receiver_uuid'")
                    vault.withdraw(e.whoClicked as Player,totalPrice)
                    e.whoClicked.sendMessage("§a§lアイテムを発送しました！")
                    Bukkit.getScheduler().runTask(plugin,Runnable{ playerLastSendingData[e.whoClicked.uniqueId]= getDateForMySQL(date).toString()})
                    (e.whoClicked as Player).playSound(e.whoClicked.location, Sound.ENTITY_PLAYER_LEVELUP,1F,0F)
                    val receiver=Bukkit.getPlayer(UUID.fromString(receiver_uuid))
                    if(receiver!=null) {
                        receiver.sendMessage("§a§lあなたに荷物が届きました！")
                        receiver.sendMessage(createClickEventText_run("§e§l§nここをクリック§r§eで受け取りましょう！","/mdrec"))
                    }
                }
                else{
                    Bukkit.getScheduler().runTask(plugin,Runnable {
                        returnItem(e.whoClicked as Player,container)
                        e.whoClicked.sendMessage("送信に失敗しました")
                    })
                }
                result.close()
                mysql.close()
            }
        }
    }

    @EventHandler
    fun containerSelectGUIClickEvent(e: InventoryClickEvent){
        val inv=e.clickedInventory?:return
        if(e.view.title()!=Component.text(Main.containerSelectGUIName)||inv.type==InventoryType.PLAYER)return
        val item=e.currentItem?:return
        e.isCancelled=true
        if(item.type==Material.WHITE_STAINED_GLASS_PANE)return
        e.whoClicked.openInventory(generateContainer(getNBTString(item,"wrapping"), inv.contents!![inv.size-1]!!))
        (e.whoClicked as Player).playSound(e.whoClicked.location,Sound.BLOCK_NOTE_BLOCK_HARP,1F,1F)
    }

    @EventHandler
    fun registerContainerUser(e: PlayerJoinEvent){
        Main.executor.execute {
            val result=mysql.query("select delivery_amount from player_status where owner_uuid='${e.player.uniqueId}'")
            if(result==null){
                mysql.close()
                return@execute
            }
            result.next()
            if(result.row==0){
                mysql.execute("insert into player_status(owner_name,owner_uuid) values('${e.player.name}','${e.player.uniqueId}');")
                result.close()
                mysql.close()
                return@execute
            }
            val amount=result.getInt("delivery_amount")
            result.close()
            val dateResult= mysql.query("select order_date from delivery_order where sender_uuid='${e.player.uniqueId}' order by order_date desc limit 1;")
            if(dateResult!=null&&dateResult.next()) {
                val lastTime=dateResult.getString("order_date")
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    playerLastSendingData[e.player.uniqueId] = lastTime
                })
            }
            dateResult?.close()
            mysql.close()
            sleep(6000)
            if(amount>0) {
                e.player.sendMessage("§a§lあなたに§d${amount}個§a§lのお届け物があります！")
                e.player.sendMessage(createClickEventText_run("§eここを§lクリック§r§eで受け取りましょう！", "/mdrec"))
            }
        }
    }

    @EventHandler
    fun deleteDateData(e:PlayerQuitEvent){
        playerLastSendingData.remove(e.player.uniqueId)
    }

    @EventHandler
    fun containerCloseEvent(e: InventoryCloseEvent){
        if(e.view.title()!= Component.text(Main.containerGUIName)||e.inventory.size!=9||e.inventory.getItem(8)?.itemMeta?.displayName()!=Component.text(sendButtonName))return
        e.inventory.contents?.let { returnItem(e.player as Player, it) }
    }

}
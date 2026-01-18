package ltotj.minecraft.man10deliveryservice.command

import ltotj.minecraft.man10deliveryservice.Main
import ltotj.minecraft.man10deliveryservice.Main.Companion.executor
import ltotj.minecraft.man10deliveryservice.Main.Companion.playerLastSendingData
import ltotj.minecraft.man10deliveryservice.Main.Companion.plugin
import ltotj.minecraft.man10deliveryservice.Main.Companion.pluginTitle
import ltotj.minecraft.man10deliveryservice.MySQLManager
import ltotj.minecraft.man10deliveryservice.Utility.createClickEventText_run
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.sql.ResultSet
import java.util.*

object LogCommand:CommandExecutor,TabCompleter {

    private val mysql = MySQLManager(plugin, pluginTitle)

    private fun createReceivingLog(resultFromSQL: ResultSet): Component {
        return if (resultFromSQL.getBoolean("order_status")) {
            createClickEventText_run("§9ID:${resultFromSQL.getInt("order_id")},受取日時:${resultFromSQL.getString("receive_date")},発送元:${resultFromSQL.getString("sender_name")}" +
                    ",受け取り状況：§d受け取り済,§e[クリックでボックスの中身を確認]", "/mdlog box ${resultFromSQL.getInt("order_id")}")
        } else {
            createClickEventText_run("§9ID:${resultFromSQL.getInt("order_id")},受取日時:${resultFromSQL.getString("receive_date")},発送元:${resultFromSQL.getString("sender_name")}" +
                    ",受け取り状況：§4未受け取り,§e[クリックで受け取り]", "/mdrec")
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("§d/mdlog <send,receive,box> (ページ数,id)")
            return true
        }
        if (sender !is Player){
            return true
        }
        if(!sender.hasPermission("mdelivery.player")){
            sender.sendMessage("§4あなたはこのコマンドを実行する権限を持っていません！")
            return true
        }
        if(!Main.available){
            sender.sendMessage("§4[$pluginTitle]はただいま停止中です")
            return true
        }
        when (args[0]) {
            "send" -> {
                val page = if (args.size == 1) 1 else args[1].toIntOrNull() ?: 1
                executor.execute {
                    val result = mysql.query("select order_id,receiver_name,order_status,order_date from delivery_order where sender_uuid='${sender.uniqueId}' order by order_id desc limit 11 offset ${(page - 1) * 10};")
                    if(result==null){
                        sender.sendMessage("データベース接続エラー")
                        mysql.close()
                        return@execute
                    }
                    result.next()
                    if (result.row == 0) {
                        sender.sendMessage("§e送信ログが存在しません")
                    } else {
                        sender.sendMessage(createClickEventText_run("§9オーダーID:${result.getInt("order_id")},発送日時:${result.getString("order_date")},発送先:${result.getString("receiver_name")} " +
                                ",受け取り状況：${if (result.getBoolean("order_status")) "§d受け取り済み" else "§4未受け取り"} ,§e[クリックで送ったアイテムを確認]", "/mdlog box ${result.getInt("order_id")}"))
                        var count = 0
                        while (count < 9 && result.next()) {
                            sender.sendMessage(createClickEventText_run("§9オーダーID:${result.getInt("order_id")},発送日時:${result.getString("order_date")}, 発送先:${result.getString("receiver_name")} " +
                                    ",受け取り状況：${if (result.getBoolean("order_status")) "§d受け取り済み" else "§4未受け取り"} ,§e[クリックで送ったアイテムを確認]", "/mdlog box ${result.getInt("order_id")}"))
                            count++
                        }
                        if (result.next()) {
                            sender.sendMessage(createClickEventText_run("§a[次のページへ]", "/mdlog send ${page + 1}"))
                        } else {
                            sender.sendMessage("§a最後のページです")
                        }
                    }
                    result.close()
                    mysql.close()
                }
            }
            "receive" -> {
                val page = if (args.size == 1) 1 else args[1].toIntOrNull() ?: 1
                executor.execute {
                    val result = mysql.query("select order_id,sender_name,order_status,receive_date from delivery_order where receiver_uuid='${sender.uniqueId}' order by order_id desc limit 11 offset ${(page - 1) * 10};")
                    if(result==null){
                        sender.sendMessage("データベース接続エラー")
                        mysql.close()
                        return@execute
                    }
                    result.next()
                    if (result.row == 0) {
                        sender.sendMessage("§e${10 * page}件以上のオーダーは存在しません")
                    } else {
                        sender.sendMessage(createReceivingLog(result))
                        var count = 0
                        while (count < 9 && result.next()) {
                            sender.sendMessage(createReceivingLog(result))
                            count++
                        }
                        if (result.next()) {
                            sender.sendMessage(createClickEventText_run("[§a次のページへ]", "/mdlog receive ${page + 1}"))
                        }
                    }
                    result.close()
                    mysql.close()
                }
            }
            "box" -> {
                if (args.size < 2) {
                    sender.sendMessage("§4IDを指定してください")
                    return true
                }
                val order_id = args[1].toIntOrNull()
                if (order_id == null || order_id < 1) {
                    sender.sendMessage("§4IDは1以上の数字を指定してください")
                } else {
                    executor.execute {
                        val result = mysql.query("select sender_uuid,receiver_uuid,slot1,slot2,slot3,slot4,slot5,slot6,slot7,slot8,box_status,opener_name from delivery_order where order_id=$order_id;")
                        if (result == null) {
                            sender.sendMessage("§4データベース接続エラー")
                            mysql.close()
                            return@execute
                        }
                        if (result.next()) {
                            if (!sender.hasPermission("mdelivery.admin")&&result.getString("sender_uuid") != sender.uniqueId.toString() && result.getString("receiver_uuid") != sender.uniqueId.toString()) {
                                sender.sendMessage("§4このボックスのログを確認する権限がありません")
                            } else {
                                sender.sendMessage("§aオーダーID：${order_id}のアイテムを検索中・・・")
                                sender.sendMessage(if(result.getBoolean("box_status")) "§e開封者:${result.getString("opener_name")}" else "§eボックス未開封")
                                for (i in 1..8) {
                                    val item = ItemStack.deserializeBytes(result.getBytes("slot$i") ?: continue)
                                    sender.sendMessage(text("§e§l[スロット$i]：${item.amount}個").hoverEvent(item.asHoverEvent()))
                                }
                                sender.sendMessage("§a検索完了  カーソルを合わせることで表示されます")
                            }
                        } else {
                            sender.sendMessage("§4オーダーが見つかりませんでした")
                        }
                        result.close()
                        mysql.close()

                    }
                }
                return true
            }
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (alias == "mdlog") {
            when(args.size) {
                1->{
                    return mutableListOf("send","receive","box")
                }
                2->{
                    when(args[0]){
                        "send","receive"->{
                            return mutableListOf("ページ数")
                        }
                        "box"->return mutableListOf("ボックスID")
                    }
                }
            }
        }
        return mutableListOf()
    }
}


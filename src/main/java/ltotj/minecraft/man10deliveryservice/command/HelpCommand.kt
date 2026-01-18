package ltotj.minecraft.man10deliveryservice.command

import ltotj.minecraft.man10deliveryservice.Main.Companion.con
import ltotj.minecraft.man10deliveryservice.Main.Companion.pluginTitle
import ltotj.minecraft.man10deliveryservice.Utility.getYenString
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

object HelpCommand:CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        sendMessage(sender,arrayOf("§6==========§e[$pluginTitle]§6==========="
                ,"§e/mdsend 相手 ->指定した相手にアイテムを配達することができます §c${getYenString(con.getDouble("postage"))}(電子マネー)§eが必要です"
                ,"§e/mdrec ->お届け物を受け取ります"
                ,"§e/mdlog <send,receive,box> (ページ数またはID) ->発送/受取/ボックスのログを確認できます"
                ,"§e/mletter １行目 ２行目 ・・・ ->手紙を作成することができます §c${getYenString(con.getDouble("letter.price"))}(電子マネー)§eが必要です"
                ,""
                ,"§e§l受け取ったボックスは右クリックで開くことができます","§6========================================"))
                return true
    }

    private fun sendMessage(sender:CommandSender,messages:Array<out String>){
        messages.forEach {
            sender.sendMessage(it)
        }
    }

}
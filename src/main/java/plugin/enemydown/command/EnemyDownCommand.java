package plugin.enemydown.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.enemydown.Data.PlayerScore;
import plugin.enemydown.Main;

public class EnemyDownCommand implements CommandExecutor, Listener {

  private  Main main;
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private int gameTime = 20;

  public EnemyDownCommand(Main main) {
    this.main =main;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if(sender instanceof Player player){
      if(playerScoreList.isEmpty()) {
        gameTime = 20;
        addNewPlayer(player);
      } else {
        for(PlayerScore playerScore : playerScoreList){
          if(!playerScore.getPlayerName().equals(player.getName())){
            addNewPlayer(player);
          }
        }
      }

      World world = player.getWorld();

      initPlayerStatus(player);

      Bukkit.getScheduler().runTaskTimer(main,Runnable ->{
        if(gameTime <= 0) {
          Runnable.cancel();
          player.sendMessage("ゲームは終了しました！");
          return;
        }
        world.spawnEntity(getEnemySpawnLocation(player, world), getEnemy());
        player.sendMessage(getEnemy().name() + "が出現しました！");
        gameTime -= 5;
      },0,5 * 20);
    }
    return false;
  }

  @EventHandler
  public void onEnemyDeath(EntityDeathEvent e){
    Player player = e.getEntity().getKiller();
    if (Objects.isNull(player) || playerScoreList.isEmpty()) {
      return;
    }

    for(PlayerScore playerScore : playerScoreList){
      if (playerScore.getPlayerName().equals(player.getName())){
        playerScore.setScore(playerScore.getScore() + 10 );
        player.sendMessage("敵を倒した！現在のスコアは" + playerScore.getScore() + "点!");
      }
    }
    }

  /**
   * ゲームを始める前にプレイヤーんの状態を設定する。
   * 体力と空腹度を最大にして、装備はネザライト一式になる。
   *
   * @param player  コマンドを実行したプレイヤー
   */
  private void initPlayerStatus(Player player) {
    player.setHealth(20);
    player.setFoodLevel(20);
    PlayerInventory playerInventory = player.getInventory();
    playerInventory.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
    playerInventory.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
    playerInventory.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
    playerInventory.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
    playerInventory.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
  }

  /**
   * 敵の出現場所を取得します。
   * 出現エリアはX軸とZ軸は自分の位置からプラス、ランダムで-10から9の値が設定されます。
   * Y軸はプレイヤーと同じ位置になります。
   *
   * @param player　コマンドを実行したプレイヤー
   * @param world　コマンドを実行したプレイヤーが所属するワールド
   * @return  敵の出現場所
   */
  private Location getEnemySpawnLocation(Player player, World world) {
    Location playerLocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(10) - 20;
    int randomZ = new SplittableRandom().nextInt(10) - 20;

    double x = playerLocation.getX() + randomX;
    double y = playerLocation.getY();
    double z = playerLocation.getZ() + randomZ;

    return new Location(world, x,y,z);
  }

  /**
   * 新規のプレイヤー情報をリストに追加
   * @param player  コマンドを実行したプレイヤー
   */
  private void addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore();
    newPlayer.setPlayerName(player.getName());
    playerScoreList.add(newPlayer);
  }
  /**
   * ランダムで敵を抽選して、その結果を取得します。
   *
   * @return  敵
   */
  private EntityType getEnemy() {
    List<EntityType> enemyList =List.of(EntityType.ZOMBIE,EntityType.SKELETON);
    return enemyList.get(new SplittableRandom().nextInt(2));
  }
}

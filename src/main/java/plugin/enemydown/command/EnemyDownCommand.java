package plugin.enemydown.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import plugin.enemydown.Data.PlayerScore;
import plugin.enemydown.Main;

/**
 * 時間制限内にランダムで出現する敵を倒して、スコアを獲得するゲームを起動するコマンドです。
 * スコアは的によって変わり、倒せた敵の合計によってスコアが変動します。
 * 結果はプレイヤー名、点数、日時などで保存されます。
 */
public class EnemyDownCommand extends BaseCommand implements Listener {

  public static final int GAME_TIME = 20;
  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";
  public static final String NONE = "none";
  private  Main main;
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private List<Entity> spawnEntityList = new ArrayList<>();

  public EnemyDownCommand(Main main) {
    this.main = main;
  }


  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label, String[] args) {
    String difficulty = getDifficulty(player, args);
    if (difficulty.equals(NONE)){
      return false;
    }

    PlayerScore nowPlayerScore = getPlayerScore(player);

    nowPlayerScore.setGameTime(GAME_TIME);

    initPlayerStatus(player);

    gamePlay(player, nowPlayerScore, difficulty);
    return true;
  }

  /**
   * 難易度をコマンド引数から取得します。
   *
   * @param player　コマンドを実行したプレイヤー
   * @param args　コマンド引数
   * @return 難易度
   */

  private String getDifficulty(Player player, String[] args) {
    if (args.length == 1 && (EASY.equals(args[0]) || NORMAL.equals(args[0]) || HARD.equals(args[0]))) {
      return args[0];
    }
    player.sendMessage(ChatColor.RED + "実行できません。コマンド引数の１つ目に難易度指定が必要です。[easy,normal,hard]");
    return NONE;
  }

  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, Command command, String label, String[] args) {
    return false;
  }

  @EventHandler
  public void onEnemyDeath(EntityDeathEvent e){
    LivingEntity enemy = e.getEntity();
    Player player = enemy.getKiller();

    if (Objects.isNull(player) || spawnEntityList.stream().noneMatch(entity -> entity.equals(enemy))) {
      return;
    }

    playerScoreList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {
          int point = switch (enemy.getType()) {
            case ZOMBIE -> 10;
            case SKELETON, WITCH -> 20;
            default -> 0;
          };
          p.setScore(p.getScore() + point);
          player.sendMessage("敵を倒した！現在のスコアは" + p.getScore() + "点!");
        });
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
    playerInventory.setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
  }

  /**
   * ゲームを実行します。基底の時間内に敵を倒すとスコアが加算されます。合計スコアを時間経過後に表示します。
   *
   * @param player          コマンドを実行したプレイヤー
   * @param nowPlayerScore  プレイヤースコア情報
   * @param difficulty      難易度
   */

  private boolean gamePlay(Player player, PlayerScore nowPlayerScore,String difficulty) {
    Bukkit.getScheduler().runTaskTimer(main,Runnable ->{
      if(nowPlayerScore.getGameTime() <= 0) {
        Runnable.cancel();

        player.sendTitle("ゲームが終了しました。",
            nowPlayerScore.getPlayerName() + " 合計 " + nowPlayerScore.getScore() + "点!",
            0,100,0);

        spawnEntityList.forEach(Entity::remove);
        spawnEntityList.clear();

        removePotionEffect(player);
        player.sendMessage("周囲の敵が消えました");
        return;
      }
      Entity spawnEntity = player.getWorld().spawnEntity(getEnemySpawnLocation(player, player.getWorld()), getEnemy(difficulty));
      spawnEntityList.add(spawnEntity);
      player.sendMessage(spawnEntity.getName() + "が出現しました！");
      nowPlayerScore.setGameTime(nowPlayerScore.getGameTime() -5 );
    },0,5 * 20);
    return true;
  }

  /**
   * 敵の出現場所を取得します。
   * 出現エリアはX軸とZ軸は自分の位置からプラス、ランダムで-3から2の値が設定されます。
   * Y軸はプレイヤーと同じ位置になります。
   *
   * @param player　コマンドを実行したプレイヤー
   * @param world　コマンドを実行したプレイヤーが所属するワールド
   * @return  敵の出現場所
   */
  private Location getEnemySpawnLocation(Player player, World world) {
    Location playerLocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(3) - 6;
    int randomZ = new SplittableRandom().nextInt(3) - 6;

    double x = playerLocation.getX() + randomX;
    double y = playerLocation.getY();
    double z = playerLocation.getZ() + randomZ;

    return new Location(player.getWorld(), x,y,z);
  }

  /**
   * 現在実行しているプレイヤーのスコア情報を取得する。
   * @param player  コマンドを実行したプレイヤー
   * @return 現在実行しているプレイヤーのスコア情報
   */
  private PlayerScore getPlayerScore(Player player) {
    PlayerScore playerScore = new PlayerScore(player.getName());
    if(playerScoreList.isEmpty()) {
      playerScore = addNewPlayer(player);
    } else {
      playerScore = playerScoreList.stream()
          .findFirst()
          .map(ps -> ps.getPlayerName().equals(player.getName())
          ? ps :
          addNewPlayer(player)).orElse(playerScore);
    }
    playerScore.setGameTime(GAME_TIME);
    playerScore.setScore(0);
    removePotionEffect(player);

    return playerScore;
  }

  /**
   * 新規のプレイヤー情報をリストに追加
   * @param player  コマンドを実行したプレイヤー
   * @return  新規プレイヤー
   * */
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore(player.getName());
    playerScoreList.add(newPlayer);
    return newPlayer;
  }
  /**
   * ランダムで敵を抽選して、その結果を取得します。
   * @pram  difficulty 難易度
   * @return  敵
   */
  private EntityType getEnemy(String difficulty) {
    List<EntityType> enemyList = switch (difficulty) {
      case NORMAL -> List.of(EntityType.ZOMBIE, EntityType.SKELETON);
      case HARD -> List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITCH);
      default -> List.of(EntityType.ZOMBIE);
    };
    return enemyList.get(new SplittableRandom().nextInt(enemyList.size()));
  }
  /**
   * プレイヤーに設定されている特殊効果を除外します。
   * @param player　コマンドを実行したプレイヤー
   */
  private void removePotionEffect(Player player) {
    player.getActivePotionEffects().stream()
        .map(PotionEffect::getType)
        .forEach(player::removePotionEffect);
  }
}

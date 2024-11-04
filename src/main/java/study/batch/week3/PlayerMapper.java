package study.batch.week3;

import lombok.extern.java.Log;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

@Log
public class PlayerMapper implements FieldSetMapper<Player> {
    @Override
    public Player mapFieldSet(FieldSet fs) {
        if (fs == null) return null;

        log.info("------------------ Reader Execute ------------------");
        Player player = new Player();
        player.setNo(fs.readLong("No"));
        player.setName(fs.readString("Name"));
        player.setAge(fs.readInt("Age"));

        return player;
    }
}

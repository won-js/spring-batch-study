package study.batch.week3;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

public class PlayerMapper implements FieldSetMapper<Player> {
    public Player mapFieldSet(FieldSet fs) {
        if (fs == null) return null;

        Player player = new Player();
        player.setNo(fs.readLong("No"));
        player.setName(fs.readString("Name"));
        player.setAge(fs.readInt("Age"));

        return player;
    }
}

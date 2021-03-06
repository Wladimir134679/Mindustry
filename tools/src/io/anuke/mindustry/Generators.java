package io.anuke.mindustry;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.ImagePacker.GenRegion;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Mech;
import io.anuke.mindustry.type.UnitType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.OreBlock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.tilesize;

public class Generators {

    public static void generate(){

        ImagePacker.generate("block-icons", () -> {
            Image colors = new Image(256, 1);
            Color outlineColor = new Color(0, 0, 0, 0.3f);

            for(Block block : content.blocks()){
                TextureRegion[] regions = block.getGeneratedIcons();

                try{
                    if(block instanceof Floor){
                        block.load();
                        for(TextureRegion region : block.variantRegions()){
                            GenRegion gen = (GenRegion)region;
                            if(gen.path == null) continue;
                            Files.copy(gen.path, Paths.get("../editor/editor-" + gen.path.getFileName()));
                        }
                    }
                }catch(IOException e){
                    throw new RuntimeException(e);
                }

                if(regions.length == 0){
                    continue;
                }

                try{
                    Image last = null;
                    if(block.outlineIcon){
                        int radius = 3;
                        GenRegion region = (GenRegion)regions[regions.length-1];
                        Image base = ImagePacker.get(region);
                        Image out = last = new Image(region.getWidth(), region.getHeight());
                        for(int x = 0; x < out.width(); x++){
                            for(int y = 0; y < out.height(); y++){

                                Color color = base.getColor(x, y);
                                if(color.a >= 0.01f){
                                    out.draw(x, y, color);
                                }else{
                                    boolean found = false;
                                    outer:
                                    for(int rx = -radius; rx <= radius; rx++){
                                        for(int ry = -radius; ry <= radius; ry++){
                                            if(Mathf.dst(rx, ry) <= radius && base.getColor(rx + x, ry + y).a > 0.01f){
                                                found = true;
                                                break outer;
                                            }
                                        }
                                    }
                                    if(found){
                                        out.draw(x, y, outlineColor);
                                    }
                                }
                            }
                        }

                        try{
                            Files.delete(region.path);
                        }catch(IOException e){
                            e.printStackTrace();
                        }

                        out.save(block.name);
                    }

                    Image image = ImagePacker.get(regions[0]);

                    int i = 0;
                    for(TextureRegion region : regions){
                        i ++;
                        if(i != regions.length || last == null){
                            image.draw(region);
                        }else{
                            image.draw(last);
                        }
                    }

                    if(regions.length > 1){
                        image.save(block.name + "-icon-full");
                    }

                    image.save("../editor/" + block.name + "-icon-editor");

                    for(Icon icon : Icon.values()){
                        if(icon.size == 0 || (icon.size == image.width() && icon.size == image.height())) continue;
                        Image scaled = new Image(icon.size, icon.size);
                        scaled.drawScaled(image);
                        scaled.save(block.name + "-icon-" + icon.name());
                    }

                    Color average = new Color();
                    for(int x = 0; x < image.width(); x++){
                        for(int y = 0; y < image.height(); y++){
                            Color color = image.getColor(x, y);
                            average.r += color.r;
                            average.g += color.g;
                            average.b += color.b;
                        }
                    }
                    average.mul(1f / (image.width() * image.height()));
                    average.a = 1f;
                    colors.draw(block.id, 0, average);
                }catch(IllegalArgumentException e){
                    Log.info("Skipping &ly'{0}'", block.name);
                }catch(NullPointerException e){
                    Log.err("Block &ly'{0}'&lr has an null region!");
                }
            }

            colors.save("../../../assets/sprites/block_colors");
        });

        ImagePacker.generate("item-icons", () -> {
            for(Item item : content.items()){
                Image base = ImagePacker.get("item-" + item.name);
                for(Item.Icon icon : Item.Icon.values()){
                    if(icon.size == base.width()) continue;
                    Image image = new Image(icon.size, icon.size);
                    image.drawScaled(base);
                    image.save("item-" + item.name + "-" + icon.name(), false);
                }
            }
        });

        ImagePacker.generate("mech-icons", () -> {
            for(Mech mech : content.<Mech>getBy(ContentType.mech)){

                mech.load();
                mech.weapon.load();

                Image image = ImagePacker.get(mech.region);

                if(!mech.flying){
                    image.drawCenter(mech.baseRegion);
                    image.drawCenter(mech.legRegion);
                    image.drawCenter(mech.legRegion, true, false);
                    image.drawCenter(mech.region);
                }

                int off = image.width()/2 - mech.weapon.region.getWidth()/2;

                image.draw(mech.weapon.region, -(int)mech.weaponOffsetX + off, (int)mech.weaponOffsetY + off + 4, false, false);
                image.draw(mech.weapon.region, (int)mech.weaponOffsetX + off, (int)mech.weaponOffsetY + off + 4, true, false);


                image.save("mech-icon-" + mech.name);
            }
        });

        ImagePacker.generate("unit-icons", () -> {
            for(UnitType type : content.<UnitType>getBy(ContentType.unit)){
                if(type.isFlying) continue;

                type.load();
                type.weapon.load();

                Image image = ImagePacker.get(type.region);

                image.draw(type.baseRegion);
                image.draw(type.legRegion);
                image.draw(type.legRegion, true, false);
                image.draw(type.region);

                image.draw(type.weapon.region,
                        -(int)type.weapon.width + image.width()/2 - type.weapon.region.getWidth()/2,
                        (int)type.weaponOffsetY - image.height()/2 - type.weapon.region.getHeight()/2 + 1,
                        false, false);
                image.draw(type.weapon.region,
                        (int)type.weapon.width + image.width()/2 - type.weapon.region.getWidth()/2,
                        (int)type.weaponOffsetY - image.height()/2 - type.weapon.region.getHeight()/2 + 1,
                        true, false);

                image.save("unit-icon-" + type.name);
            }
        });

        ImagePacker.generate("ore-icons", () -> {
            for(Block block : content.blocks()){
                if(!(block instanceof OreBlock)) continue;

                OreBlock ore = (OreBlock)block;
                Item item = ore.itemDrop;
                Block base = ore.base;

                for (int i = 0; i < 3; i++) {
                    //get base image to draw on
                    Image image = ImagePacker.get(base.name + (i+1));
                    Image shadow = ImagePacker.get(item.name + (i+1));

                    int offset = image.width()/tilesize;

                    for (int x = 0; x < image.width(); x++) {
                        for (int y = offset; y < image.height(); y++) {
                            Color color = shadow.getColor(x, y - offset);

                            //draw semi transparent background
                            if(color.a > 0.001f){
                                color.set(0, 0, 0, 0.3f);
                                image.draw(x, y, color);
                            }
                        }
                    }

                    image.draw(ImagePacker.get(item.name + (i+1)));
                    image.save("../blocks/environment/ore-" + item.name + "-" + base.name + (i+1));
                    image.save("../editor/editor-ore-" + item.name + "-" + base.name + (i+1));

                    //save icons
                    image.save(block.name + "-icon-full");
                    for(Icon icon : Icon.values()){
                        if(icon.size == 0) continue;
                        Image scaled = new Image(icon.size, icon.size);
                        scaled.drawScaled(image);
                        scaled.save(block.name + "-icon-" + icon.name());
                    }
                }
            }
        });

        ImagePacker.generate("edges", () -> {
            for(Block block : content.blocks()){
                if(!(block instanceof Floor)) continue;

                Floor floor = (Floor)block;

                if(ImagePacker.has(floor.name + "-edge") || floor.blendGroup != floor){
                    continue;
                }

                try{
                    Image image = ImagePacker.get(floor.generateIcons()[0]);
                    Image edge = ImagePacker.get("edge-stencil-" + floor.edgeStyle);
                    Image result = new Image(edge.width(), edge.height());

                    for(int x = 0; x < edge.width(); x++){
                        for(int y = 0; y < edge.height(); y++){
                            result.draw(x, y, edge.getColor(x, y).mul(image.getColor(x % image.width(), y % image.height())));
                        }
                    }

                    result.save("../blocks/environment/" + floor.name + "-edge");

                }catch(Exception ignored){}
            }
        });
    }

}

import { Composition } from "remotion";
import { PromoVideo } from "./PromoVideo";

export const RemotionRoot = () => {
  return (
    <Composition
      id="SceneSensePromo"
      component={PromoVideo}
      durationInFrames={900}
      fps={30}
      width={1080}
      height={1920}
    />
  );
};

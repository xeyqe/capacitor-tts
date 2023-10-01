export interface TTSPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
